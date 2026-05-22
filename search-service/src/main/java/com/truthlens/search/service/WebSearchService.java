package com.truthlens.search.service;

import com.truthlens.search.exception.ExternalSearchException;
import com.truthlens.search.model.DuckDuckGoApiResponse;
import com.truthlens.search.model.SearchExecutionRequest;
import com.truthlens.search.model.SearchExecutionResponse;
import com.truthlens.search.model.SearchResultItem;
import com.truthlens.search.model.WikipediaApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WebSearchService {

    private static final Logger log = LoggerFactory.getLogger(WebSearchService.class);

    private final RestClient duckDuckGoRestClient;
    private final RestClient wikipediaRestClient;

    public WebSearchService(RestClient duckDuckGoRestClient, RestClient wikipediaRestClient) {
        this.duckDuckGoRestClient = duckDuckGoRestClient;
        this.wikipediaRestClient = wikipediaRestClient;
    }

    public SearchExecutionResponse executeSearch(SearchExecutionRequest request) {
        if (request.queries() == null || request.queries().isEmpty()) {
            throw new IllegalArgumentException("At least one search query is required");
        }

        int maxPerQuery = request.maxResultsPerQuery() > 0 ? request.maxResultsPerQuery() : 3;

        Map<String, SearchResultItem> deduplicatedResults = new LinkedHashMap<>();

        for (String query : request.queries()) {
            log.info("Searching DuckDuckGo for: \"{}\"", query);
            List<SearchResultItem> queryResults = searchDuckDuckGo(query, maxPerQuery);
            
            if (queryResults.isEmpty()) {
                log.info("DuckDuckGo returned empty results for \"{}\". Falling back to Wikipedia.", query);
                queryResults = searchWikipedia(query, maxPerQuery);
            }
            
            if (queryResults.isEmpty()) {
                queryResults = List.of(createFallbackResult(query));
            }

            for (SearchResultItem item : queryResults) {
                deduplicatedResults.putIfAbsent(item.url(), item);
            }
        }

        List<SearchResultItem> results = new ArrayList<>(deduplicatedResults.values());
        log.info("Total deduplicated results: {}", results.size());

        return new SearchExecutionResponse(results);
    }

    @Retryable(
            retryFor = ExternalSearchException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public List<SearchResultItem> searchDuckDuckGo(String query, int maxResults) {
        try {
            DuckDuckGoApiResponse response = duckDuckGoRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("q", query)
                            .queryParam("format", "json")
                            .queryParam("no_html", "1")
                            .queryParam("skip_disambig", "1")
                            .build())
                    .retrieve()
                    .body(DuckDuckGoApiResponse.class);

            if (response == null) {
                return List.of();
            }

            return extractDuckDuckGoResults(response, maxResults);

        } catch (HttpClientErrorException.TooManyRequests ex) {
            throw new ExternalSearchException("DuckDuckGo rate limit exceeded (429) for query: " + query, ex);
        } catch (HttpServerErrorException.ServiceUnavailable ex) {
            throw new ExternalSearchException("DuckDuckGo service unavailable (503) for query: " + query, ex);
        } catch (HttpServerErrorException | HttpClientErrorException ex) {
            throw new ExternalSearchException("DuckDuckGo API error (%d) for query: %s".formatted(ex.getStatusCode().value(), query), ex);
        } catch (Exception ex) {
            log.error("Unexpected error calling DuckDuckGo for query: \"{}\"", query, ex);
            throw new ExternalSearchException("Unexpected error calling DuckDuckGo for query: " + query, ex);
        }
    }

    @Retryable(
            retryFor = ExternalSearchException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public List<SearchResultItem> searchWikipedia(String query, int maxResults) {
        try {
            WikipediaApiResponse response = wikipediaRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("action", "query")
                            .queryParam("list", "search")
                            .queryParam("srsearch", query)
                            .queryParam("utf8", "")
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .body(WikipediaApiResponse.class);

            if (response == null || response.query() == null || response.query().search() == null) {
                return List.of();
            }

            return extractWikipediaResults(response, maxResults);

        } catch (HttpClientErrorException.TooManyRequests ex) {
            throw new ExternalSearchException("Wikipedia rate limit exceeded (429) for query: " + query, ex);
        } catch (HttpServerErrorException.ServiceUnavailable ex) {
            throw new ExternalSearchException("Wikipedia service unavailable (503) for query: " + query, ex);
        } catch (HttpServerErrorException | HttpClientErrorException ex) {
            throw new ExternalSearchException("Wikipedia API error (%d) for query: %s".formatted(ex.getStatusCode().value(), query), ex);
        } catch (Exception ex) {
            log.error("Unexpected error calling Wikipedia for query: \"{}\"", query, ex);
            throw new ExternalSearchException("Unexpected error calling Wikipedia for query: " + query, ex);
        }
    }

    private List<SearchResultItem> extractDuckDuckGoResults(DuckDuckGoApiResponse response, int maxResults) {
        List<SearchResultItem> items = new ArrayList<>();

        if (response.AbstractText() != null && !response.AbstractText().isBlank()) {
            items.add(new SearchResultItem(
                    response.Heading() != null ? response.Heading() : "Abstract",
                    response.AbstractURL() != null ? response.AbstractURL() : "",
                    response.AbstractText()
            ));
        }

        if (response.Answer() != null && !response.Answer().isBlank()) {
            items.add(new SearchResultItem("Direct Answer", "", response.Answer()));
        }

        if (response.Definition() != null && !response.Definition().isBlank()) {
            items.add(new SearchResultItem(
                    response.DefinitionSource() != null ? response.DefinitionSource() : "Definition",
                    response.DefinitionURL() != null ? response.DefinitionURL() : "",
                    response.Definition()
            ));
        }

        if (response.RelatedTopics() != null) {
            for (DuckDuckGoApiResponse.RelatedTopic topic : response.RelatedTopics()) {
                if (items.size() >= maxResults) break;

                if (topic.Text() != null && !topic.Text().isBlank() && topic.FirstURL() != null && !topic.FirstURL().isBlank()) {
                    items.add(new SearchResultItem(extractTitle(topic.Text()), topic.FirstURL(), topic.Text()));
                }

                if (topic.Topics() != null) {
                    for (DuckDuckGoApiResponse.RelatedTopic nested : topic.Topics()) {
                        if (items.size() >= maxResults) break;

                        if (nested.Text() != null && !nested.Text().isBlank() && nested.FirstURL() != null && !nested.FirstURL().isBlank()) {
                            items.add(new SearchResultItem(extractTitle(nested.Text()), nested.FirstURL(), nested.Text()));
                        }
                    }
                }
            }
        }

        return items.size() > maxResults ? items.subList(0, maxResults) : items;
    }

    private List<SearchResultItem> extractWikipediaResults(WikipediaApiResponse response, int maxResults) {
        List<SearchResultItem> items = new ArrayList<>();

        for (WikipediaApiResponse.SearchItem item : response.query().search()) {
            if (items.size() >= maxResults) break;

            String title = item.title();
            String snippetHtml = item.snippet();
            String snippetText = snippetHtml != null ? snippetHtml.replaceAll("<[^>]*>", "") : "";
            String url = "https://en.wikipedia.org/wiki/" + URLEncoder.encode(title.replace(" ", "_"), StandardCharsets.UTF_8);

            items.add(new SearchResultItem(title, url, snippetText));
        }

        return items;
    }

    private SearchResultItem createFallbackResult(String query) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return new SearchResultItem(
                "Search Results on DuckDuckGo",
                "https://duckduckgo.com/?q=" + encodedQuery,
                "No direct API summary available. Click the link to see full DuckDuckGo search results."
        );
    }

    private String extractTitle(String text) {
        if (text == null) return "Search Result";
        int dashIndex = text.indexOf(" - ");
        if (dashIndex > 0 && dashIndex < 80) {
            return text.substring(0, dashIndex);
        }
        return text.length() > 60 ? text.substring(0, 60) + "..." : text;
    }
}
