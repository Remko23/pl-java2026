package com.truthlens.search.service;

import com.truthlens.search.exception.ExternalSearchException;
import com.truthlens.search.model.DuckDuckGoApiResponse;
import com.truthlens.search.model.SearchExecutionRequest;
import com.truthlens.search.model.SearchExecutionResponse;
import com.truthlens.search.model.SearchResultItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSearchServiceTest {

    @Mock
    private RestClient duckDuckGoRestClient;

    @Mock
    private RestClient wikipediaRestClient;

    @Mock
    private RestClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private WebSearchService webSearchService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        webSearchService = new WebSearchService(duckDuckGoRestClient, wikipediaRestClient);
        lenient().when(duckDuckGoRestClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        lenient().when(wikipediaRestClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(any(Function.class))).thenAnswer(invocation -> {
            Function<org.springframework.web.util.UriBuilder, java.net.URI> function = invocation.getArgument(0);
            function.apply(new org.springframework.web.util.DefaultUriBuilderFactory().builder());
            return requestHeadersSpec;
        });
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Nested
    @DisplayName("executeSearch — happy paths")
    class HappyPaths {

        @Test
        @DisplayName("should return results from abstract and related topics")
        void shouldReturnAbstractAndRelatedTopics() {
            var apiResponse = new DuckDuckGoApiResponse(
                    "Abstract text",
                    "Detailed abstract text about the topic",
                    "Wikipedia",
                    "https://en.wikipedia.org/wiki/Test",
                    "Test Topic",
                    null, null, null, null, null,
                    List.of(
                            new DuckDuckGoApiResponse.RelatedTopic(
                                    "Related topic 1 - description of first result",
                                    "https://example.com/1",
                                    null, null, null
                            ),
                            new DuckDuckGoApiResponse.RelatedTopic(
                                    "Related topic 2 - description of second result",
                                    "https://example.com/2",
                                    null, null, null
                            )
                    )
            );

            when(responseSpec.body(DuckDuckGoApiResponse.class)).thenReturn(apiResponse);

            var request = new SearchExecutionRequest(List.of("test query"), 5);
            SearchExecutionResponse response = webSearchService.executeSearch(request);

            assertThat(response.results()).isNotEmpty();
            assertThat(response.results()).hasSizeGreaterThanOrEqualTo(1);

            // First result should be the abstract
            SearchResultItem first = response.results().get(0);
            assertThat(first.title()).isEqualTo("Test Topic");
            assertThat(first.url()).isEqualTo("https://en.wikipedia.org/wiki/Test");
            assertThat(first.snippet()).isEqualTo("Detailed abstract text about the topic");
        }

        @Test
        @DisplayName("should deduplicate results by URL across multiple queries")
        void shouldDeduplicateByUrl() {
            var apiResponse = new DuckDuckGoApiResponse(
                    "Abstract", "Abstract text", "Source",
                    "https://example.com/shared",
                    "Heading", null, null, null, null, null,
                    List.of()
            );

            when(responseSpec.body(DuckDuckGoApiResponse.class)).thenReturn(apiResponse);

            var request = new SearchExecutionRequest(
                    List.of("query 1", "query 2"),
                    3
            );

            SearchExecutionResponse response = webSearchService.executeSearch(request);

            // Both queries return the same URL, should be deduplicated
            long distinctUrls = response.results().stream()
                    .map(SearchResultItem::url)
                    .distinct()
                    .count();

            assertThat(distinctUrls).isEqualTo(response.results().size());
        }

        @Test
        @DisplayName("should return fallback link when APIs return no data")
        void shouldReturnEmptyOnNoData() {
            var emptyResponse = new DuckDuckGoApiResponse(
                    null, null, null, null, null,
                    null, null, null, null, null,
                    List.of()
            );

            when(responseSpec.body(DuckDuckGoApiResponse.class)).thenReturn(emptyResponse);

            var request = new SearchExecutionRequest(List.of("obscure query"), 3);
            SearchExecutionResponse response = webSearchService.executeSearch(request);

            assertThat(response.results()).hasSize(1);
            assertThat(response.results().get(0).title()).isEqualTo("Search Results on DuckDuckGo");
            assertThat(response.results().get(0).url()).contains("obscure+query");
        }

        @Test
        @DisplayName("should respect maxResultsPerQuery limit")
        void shouldRespectMaxResults() {
            var apiResponse = new DuckDuckGoApiResponse(
                    "Abstract", "Abstract text", "Source",
                    "https://example.com/abstract", "Heading",
                    "Direct answer", null, null, null, null,
                    List.of(
                            new DuckDuckGoApiResponse.RelatedTopic(
                                    "Topic 1", "https://example.com/1", null, null, null),
                            new DuckDuckGoApiResponse.RelatedTopic(
                                    "Topic 2", "https://example.com/2", null, null, null),
                            new DuckDuckGoApiResponse.RelatedTopic(
                                    "Topic 3", "https://example.com/3", null, null, null)
                    )
            );

            when(responseSpec.body(DuckDuckGoApiResponse.class)).thenReturn(apiResponse);

            var request = new SearchExecutionRequest(List.of("query"), 2);
            SearchExecutionResponse response = webSearchService.executeSearch(request);

            assertThat(response.results()).hasSizeLessThanOrEqualTo(2);
        }

        @Test
        @DisplayName("should handle null response from APIs gracefully and return fallback")
        void shouldHandleNullResponse() {
            when(responseSpec.body(DuckDuckGoApiResponse.class)).thenReturn(null);

            var request = new SearchExecutionRequest(List.of("query"), 3);
            SearchExecutionResponse response = webSearchService.executeSearch(request);

            assertThat(response.results()).hasSize(1);
            assertThat(response.results().get(0).title()).isEqualTo("Search Results on DuckDuckGo");
            assertThat(response.results().get(0).url()).contains("query");
        }
    }

    @Nested
    @DisplayName("executeSearch — error handling")
    class ErrorHandling {

        @Test
        @DisplayName("should throw ExternalSearchException on 429 Too Many Requests")
        void shouldThrowOn429() {
            when(responseSpec.body(DuckDuckGoApiResponse.class))
                    .thenThrow(HttpClientErrorException.create(
                            HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests",
                            org.springframework.http.HttpHeaders.EMPTY,
                            new byte[0], null));

            var request = new SearchExecutionRequest(List.of("query"), 3);

            assertThatThrownBy(() -> webSearchService.executeSearch(request))
                    .isInstanceOf(ExternalSearchException.class)
                    .hasMessageContaining("429");
        }

        @Test
        @DisplayName("should throw ExternalSearchException on 503 Service Unavailable")
        void shouldThrowOn503() {
            when(responseSpec.body(DuckDuckGoApiResponse.class))
                    .thenThrow(HttpServerErrorException.create(
                            HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable",
                            org.springframework.http.HttpHeaders.EMPTY,
                            new byte[0], null));

            var request = new SearchExecutionRequest(List.of("query"), 3);

            assertThatThrownBy(() -> webSearchService.executeSearch(request))
                    .isInstanceOf(ExternalSearchException.class)
                    .hasMessageContaining("503");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when queries list is empty")
        void shouldThrowOnEmptyQueries() {
            var request = new SearchExecutionRequest(List.of(), 3);

            assertThatThrownBy(() -> webSearchService.executeSearch(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least one search query");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when queries list is null")
        void shouldThrowOnNullQueries() {
            var request = new SearchExecutionRequest(null, 3);

            assertThatThrownBy(() -> webSearchService.executeSearch(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least one search query");
        }
    }

    @Nested
    @DisplayName("extractResults — nested topics")
    class NestedTopics {

        @Test
        @DisplayName("should extract results from nested topic groups")
        void shouldExtractNestedTopics() {
            var nestedTopics = List.of(
                    new DuckDuckGoApiResponse.RelatedTopic(
                            "Nested 1", "https://example.com/nested1", null, null, null),
                    new DuckDuckGoApiResponse.RelatedTopic(
                            "Nested 2", "https://example.com/nested2", null, null, null)
            );

            var apiResponse = new DuckDuckGoApiResponse(
                    null, null, null, null, null,
                    null, null, null, null, null,
                    List.of(
                            new DuckDuckGoApiResponse.RelatedTopic(
                                    null, null, null, nestedTopics, "Category")
                    )
            );

            when(responseSpec.body(DuckDuckGoApiResponse.class)).thenReturn(apiResponse);

            var request = new SearchExecutionRequest(List.of("query"), 5);
            SearchExecutionResponse response = webSearchService.executeSearch(request);

            assertThat(response.results()).hasSize(2);
            assertThat(response.results().get(0).url()).isEqualTo("https://example.com/nested1");
            assertThat(response.results().get(1).url()).isEqualTo("https://example.com/nested2");
        }
    }

    @Nested
    @DisplayName("extractResults — definition and direct answer")
    class DefinitionAndAnswer {

        @Test
        @DisplayName("should extract definition result with source and URL")
        void shouldExtractDefinition() {
            var apiResponse = new DuckDuckGoApiResponse(
                    null, null, null, null, null,
                    null, null,
                    "A vaccine is a biological preparation that provides immunity.",
                    "Merriam-Webster",
                    "https://www.merriam-webster.com/dictionary/vaccine",
                    List.of()
            );

            when(responseSpec.body(DuckDuckGoApiResponse.class)).thenReturn(apiResponse);

            var request = new SearchExecutionRequest(List.of("define vaccine"), 3);
            SearchExecutionResponse response = webSearchService.executeSearch(request);

            assertThat(response.results()).hasSize(1);
            SearchResultItem item = response.results().get(0);
            assertThat(item.title()).isEqualTo("Merriam-Webster");
            assertThat(item.url()).isEqualTo("https://www.merriam-webster.com/dictionary/vaccine");
            assertThat(item.snippet()).contains("biological preparation");
        }

        @Test
        @DisplayName("should extract direct answer from API response")
        void shouldExtractDirectAnswer() {
            var apiResponse = new DuckDuckGoApiResponse(
                    null, null, null, null, null,
                    "42", "calc",
                    null, null, null,
                    List.of()
            );

            when(responseSpec.body(DuckDuckGoApiResponse.class)).thenReturn(apiResponse);

            var request = new SearchExecutionRequest(List.of("what is 6 times 7"), 3);
            SearchExecutionResponse response = webSearchService.executeSearch(request);

            assertThat(response.results()).hasSize(1);
            assertThat(response.results().get(0).title()).isEqualTo("Direct Answer");
            assertThat(response.results().get(0).snippet()).isEqualTo("42");
        }
    }

    @Nested
    @DisplayName("executeSearch — combined result types")
    class CombinedResults {

        @Test
        @DisplayName("should return results in priority order: abstract → answer → definition → topics")
        void shouldReturnResultsInPriorityOrder() {
            var apiResponse = new DuckDuckGoApiResponse(
                    "Abstract about topic",
                    "Detailed abstract text",
                    "Wikipedia",
                    "https://en.wikipedia.org/wiki/Topic",
                    "Topic Heading",
                    "Quick answer text", "factoid",
                    "Formal definition of the topic",
                    "Oxford Dictionary",
                    "https://oxford.com/topic",
                    List.of(
                            new DuckDuckGoApiResponse.RelatedTopic(
                                    "Related info - additional context",
                                    "https://example.com/related",
                                    null, null, null
                            )
                    )
            );

            when(responseSpec.body(DuckDuckGoApiResponse.class)).thenReturn(apiResponse);

            var request = new SearchExecutionRequest(List.of("query"), 10);
            SearchExecutionResponse response = webSearchService.executeSearch(request);

            assertThat(response.results()).hasSize(4);
            // Verify priority order
            assertThat(response.results().get(0).title()).isEqualTo("Topic Heading");       // abstract
            assertThat(response.results().get(1).title()).isEqualTo("Direct Answer");        // answer
            assertThat(response.results().get(2).title()).isEqualTo("Oxford Dictionary");    // definition
            assertThat(response.results().get(3).url()).isEqualTo("https://example.com/related"); // topic
        }

        @Test
        @DisplayName("should use default maxResultsPerQuery=3 when value is 0 or negative")
        void shouldDefaultMaxResultsToThree() {
            var apiResponse = new DuckDuckGoApiResponse(
                    "Abstract", "Abstract text", "Source",
                    "https://example.com/1", "Heading",
                    "Answer", null,
                    "Definition", "Dict", "https://example.com/2",
                    List.of(
                            new DuckDuckGoApiResponse.RelatedTopic(
                                    "Topic 1", "https://example.com/3", null, null, null),
                            new DuckDuckGoApiResponse.RelatedTopic(
                                    "Topic 2", "https://example.com/4", null, null, null)
                    )
            );

            when(responseSpec.body(DuckDuckGoApiResponse.class)).thenReturn(apiResponse);

            // maxResultsPerQuery = 0 → powinno zostać zamienione na domyślne 3
            var request = new SearchExecutionRequest(List.of("query"), 0);
            SearchExecutionResponse response = webSearchService.executeSearch(request);

            assertThat(response.results()).hasSizeLessThanOrEqualTo(3);
        }
    }

    @Nested
    @DisplayName("executeSearch — multiple queries scenario")
    class MultipleQueries {

        @Test
        @DisplayName("should aggregate results from multiple different queries")
        void shouldAggregateMultipleQueryResults() {
            var response1 = new DuckDuckGoApiResponse(
                    "Abstract1", "First topic abstract", "Wikipedia",
                    "https://en.wikipedia.org/wiki/First", "First Topic",
                    null, null, null, null, null,
                    List.of()
            );

            var response2 = new DuckDuckGoApiResponse(
                    "Abstract2", "Second topic abstract", "Wikipedia",
                    "https://en.wikipedia.org/wiki/Second", "Second Topic",
                    null, null, null, null, null,
                    List.of()
            );

            when(responseSpec.body(DuckDuckGoApiResponse.class))
                    .thenReturn(response1)
                    .thenReturn(response2);

            var request = new SearchExecutionRequest(
                    List.of("first query", "second query"),
                    3
            );

            SearchExecutionResponse response = webSearchService.executeSearch(request);

            // Powinny być 2 wyniki — po jednym z każdego zapytania (różne URL)
            assertThat(response.results()).hasSize(2);
            assertThat(response.results().get(0).title()).isEqualTo("First Topic");
            assertThat(response.results().get(1).title()).isEqualTo("Second Topic");
        }
    }

    @Nested
    @DisplayName("extractTitle — title parsing logic")
    class TitleExtraction {

        @Test
        @DisplayName("should extract title from text with ' - ' separator")
        void shouldExtractTitleFromDashSeparator() {
            var apiResponse = new DuckDuckGoApiResponse(
                    null, null, null, null, null,
                    null, null, null, null, null,
                    List.of(
                            new DuckDuckGoApiResponse.RelatedTopic(
                                    "Climate Change - Climate change refers to long-term shifts in temperatures and weather patterns",
                                    "https://example.com/climate",
                                    null, null, null
                            )
                    )
            );

            when(responseSpec.body(DuckDuckGoApiResponse.class)).thenReturn(apiResponse);

            var request = new SearchExecutionRequest(List.of("climate change"), 3);
            SearchExecutionResponse response = webSearchService.executeSearch(request);

            assertThat(response.results()).hasSize(1);
            // Tytuł powinien być wyciągnięty z tekstu przed " - "
            assertThat(response.results().get(0).title()).isEqualTo("Climate Change");
        }

        @Test
        @DisplayName("should truncate very long text without separator to 60 chars")
        void shouldTruncateLongTextWithoutSeparator() {
            String longText = "A".repeat(100); // 100 znaków bez " - "

            var apiResponse = new DuckDuckGoApiResponse(
                    null, null, null, null, null,
                    null, null, null, null, null,
                    List.of(
                            new DuckDuckGoApiResponse.RelatedTopic(
                                    longText,
                                    "https://example.com/long",
                                    null, null, null
                            )
                    )
            );

            when(responseSpec.body(DuckDuckGoApiResponse.class)).thenReturn(apiResponse);

            var request = new SearchExecutionRequest(List.of("query"), 3);
            SearchExecutionResponse response = webSearchService.executeSearch(request);

            assertThat(response.results()).hasSize(1);
            // Tytuł powinien mieć max 60 znaków + "..."
            assertThat(response.results().get(0).title()).hasSize(63); // 60 + "..."
            assertThat(response.results().get(0).title()).endsWith("...");
        }
    }

    @Nested
    @DisplayName("executeSearch — additional error scenarios")
    class AdditionalErrors {

        @Test
        @DisplayName("should throw ExternalSearchException on generic 500 Internal Server Error")
        void shouldThrowOn500() {
            when(responseSpec.body(DuckDuckGoApiResponse.class))
                    .thenThrow(HttpServerErrorException.create(
                            HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                            org.springframework.http.HttpHeaders.EMPTY,
                            new byte[0], null));

            var request = new SearchExecutionRequest(List.of("query"), 3);

            assertThatThrownBy(() -> webSearchService.executeSearch(request))
                    .isInstanceOf(ExternalSearchException.class)
                    .hasMessageContaining("500");
        }

        @Test
        @DisplayName("should wrap unexpected RuntimeException in ExternalSearchException")
        void shouldWrapUnexpectedRuntimeException() {
            when(responseSpec.body(DuckDuckGoApiResponse.class))
                    .thenThrow(new RuntimeException("Connection reset"));

            var request = new SearchExecutionRequest(List.of("query"), 3);

            assertThatThrownBy(() -> webSearchService.executeSearch(request))
                    .isInstanceOf(ExternalSearchException.class)
                    .hasMessageContaining("Unexpected error")
                    .hasCauseInstanceOf(RuntimeException.class);
        }
    }
}
