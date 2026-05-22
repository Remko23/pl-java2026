package com.truthlens.search.service;

import com.truthlens.search.exception.ExternalSearchException;
import com.truthlens.search.model.SearchExecutionRequest;
import com.truthlens.search.model.SearchResultItem;
import com.truthlens.search.model.WikipediaApiResponse;
import org.junit.jupiter.api.BeforeEach;
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
class WebSearchServiceEdgeCasesTest {

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
        lenient().when(requestHeadersUriSpec.uri(any(Function.class))).thenAnswer(invocation -> {
            Function<org.springframework.web.util.UriBuilder, java.net.URI> function = invocation.getArgument(0);
            function.apply(new org.springframework.web.util.DefaultUriBuilderFactory().builder());
            return requestHeadersSpec;
        });
    }

    @Test
    void testNullQueries() {
        SearchExecutionRequest request = new SearchExecutionRequest(null, 3);
        assertThatThrownBy(() -> webSearchService.executeSearch(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one search query is required");
    }

    @Test
    void testEmptyQueries() {
        SearchExecutionRequest request = new SearchExecutionRequest(List.of(), 3);
        assertThatThrownBy(() -> webSearchService.executeSearch(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one search query is required");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testWikipediaSuccess() {
        lenient().when(duckDuckGoRestClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        lenient().when(wikipediaRestClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn((RestClient.RequestHeadersSpec) requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        WikipediaApiResponse response = new WikipediaApiResponse(
                new WikipediaApiResponse.Query(List.of(
                        new WikipediaApiResponse.SearchItem("Wiki Title", "<b>Snippet</b>")
                ))
        );
        when(responseSpec.body(WikipediaApiResponse.class)).thenReturn(response);
        // Let DDG fail or return null
        when(responseSpec.body(com.truthlens.search.model.DuckDuckGoApiResponse.class)).thenReturn(null);

        SearchExecutionRequest request = new SearchExecutionRequest(List.of("query"), 3);
        var results = webSearchService.executeSearch(request).results();
        assertThat(results).hasSize(1);
        assertThat(results.get(0).title()).isEqualTo("Wiki Title");
        assertThat(results.get(0).snippet()).isEqualTo("Snippet"); // stripped HTML
    }

    @Test
    @SuppressWarnings("unchecked")
    void testWikipediaExceptions() {
        lenient().when(wikipediaRestClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn((RestClient.RequestHeadersSpec) requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        // 429
        doThrow(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "429", null, null, null))
                .when(responseSpec).body(WikipediaApiResponse.class);
        assertThatThrownBy(() -> webSearchService.searchWikipedia("query", 3))
                .isInstanceOf(ExternalSearchException.class)
                .hasMessageContaining("429");

        // 503
        doThrow(HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "503", null, null, null))
                .when(responseSpec).body(WikipediaApiResponse.class);
        assertThatThrownBy(() -> webSearchService.searchWikipedia("query", 3))
                .isInstanceOf(ExternalSearchException.class)
                .hasMessageContaining("503");

        // 500
        doThrow(HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "500", null, null, null))
                .when(responseSpec).body(WikipediaApiResponse.class);
        assertThatThrownBy(() -> webSearchService.searchWikipedia("query", 3))
                .isInstanceOf(ExternalSearchException.class)
                .hasMessageContaining("500");

        // RuntimeException
        doThrow(new RuntimeException("error"))
                .when(responseSpec).body(WikipediaApiResponse.class);
        assertThatThrownBy(() -> webSearchService.searchWikipedia("query", 3))
                .isInstanceOf(ExternalSearchException.class)
                .hasMessageContaining("Unexpected error");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDuckDuckGoExceptions() {
        lenient().when(duckDuckGoRestClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn((RestClient.RequestHeadersSpec) requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        // 429
        doThrow(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "429", null, null, null))
                .when(responseSpec).body(com.truthlens.search.model.DuckDuckGoApiResponse.class);
        assertThatThrownBy(() -> webSearchService.searchDuckDuckGo("query", 3))
                .isInstanceOf(ExternalSearchException.class)
                .hasMessageContaining("429");

        // 503
        doThrow(HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "503", null, null, null))
                .when(responseSpec).body(com.truthlens.search.model.DuckDuckGoApiResponse.class);
        assertThatThrownBy(() -> webSearchService.searchDuckDuckGo("query", 3))
                .isInstanceOf(ExternalSearchException.class)
                .hasMessageContaining("503");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testWikipediaBranchCoverage() {
        lenient().when(duckDuckGoRestClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        lenient().when(wikipediaRestClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(any(Function.class))).thenAnswer(invocation -> {
            Function<org.springframework.web.util.UriBuilder, java.net.URI> function = invocation.getArgument(0);
            function.apply(new org.springframework.web.util.DefaultUriBuilderFactory().builder());
            return requestHeadersSpec;
        });
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        // Let DDG fail or return null
        when(responseSpec.body(com.truthlens.search.model.DuckDuckGoApiResponse.class)).thenReturn(null);
        
        // 1. response is not null, query is null
        WikipediaApiResponse responseNoQuery = new WikipediaApiResponse(null);
        when(responseSpec.body(WikipediaApiResponse.class)).thenReturn(responseNoQuery);
        var res1 = webSearchService.executeSearch(new SearchExecutionRequest(List.of("query"), 3)).results();
        assertThat(res1).hasSize(1);
        assertThat(res1.get(0).title()).isEqualTo("Search Results on DuckDuckGo");

        // 2. query is not null, search is null
        WikipediaApiResponse responseNoSearch = new WikipediaApiResponse(new WikipediaApiResponse.Query(null));
        when(responseSpec.body(WikipediaApiResponse.class)).thenReturn(responseNoSearch);
        var res2 = webSearchService.executeSearch(new SearchExecutionRequest(List.of("query"), 3)).results();
        assertThat(res2).hasSize(1);
        assertThat(res2.get(0).title()).isEqualTo("Search Results on DuckDuckGo");

        // 3. snippet is null
        WikipediaApiResponse responseNullSnippet = new WikipediaApiResponse(new WikipediaApiResponse.Query(List.of(
                new WikipediaApiResponse.SearchItem("Title", null)
        )));
        when(responseSpec.body(WikipediaApiResponse.class)).thenReturn(responseNullSnippet);
        var res = webSearchService.executeSearch(new SearchExecutionRequest(List.of("query"), 3)).results();
        assertThat(res.get(0).snippet()).isEmpty();

        // 4. break condition inside Wikipedia search
        WikipediaApiResponse responseBreak = new WikipediaApiResponse(new WikipediaApiResponse.Query(List.of(
                new WikipediaApiResponse.SearchItem("Title1", "snippet1"),
                new WikipediaApiResponse.SearchItem("Title2", "snippet2")
        )));
        when(responseSpec.body(WikipediaApiResponse.class)).thenReturn(responseBreak);
        var resBreak = webSearchService.executeSearch(new SearchExecutionRequest(List.of("query"), 1)).results();
        assertThat(resBreak).hasSize(1);
    }

    private void setupMockClients() {
        lenient().when(duckDuckGoRestClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        lenient().when(wikipediaRestClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(any(Function.class))).thenAnswer(invocation -> {
            Function<org.springframework.web.util.UriBuilder, java.net.URI> function = invocation.getArgument(0);
            function.apply(new org.springframework.web.util.DefaultUriBuilderFactory().builder());
            return requestHeadersSpec;
        });
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDuckDuckGoBranchCoverage_Blank() {
        setupMockClients();
        com.truthlens.search.model.DuckDuckGoApiResponse ddgBlank = new com.truthlens.search.model.DuckDuckGoApiResponse(
                null, "", null, null, "heading", "", null, "", null, null, null
        );
        when(responseSpec.body(com.truthlens.search.model.DuckDuckGoApiResponse.class)).thenReturn(ddgBlank);
        when(responseSpec.body(WikipediaApiResponse.class)).thenReturn(null);
        var resBlank = webSearchService.executeSearch(new SearchExecutionRequest(List.of("query"), 3)).results();
        assertThat(resBlank).hasSize(1);
        assertThat(resBlank.get(0).title()).isEqualTo("Search Results on DuckDuckGo");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDuckDuckGoBranchCoverage_Topics() {
        setupMockClients();
        com.truthlens.search.model.DuckDuckGoApiResponse.RelatedTopic blankTopic = 
                new com.truthlens.search.model.DuckDuckGoApiResponse.RelatedTopic(null, "url", null, null, null);
        com.truthlens.search.model.DuckDuckGoApiResponse.RelatedTopic nestedTopicBlank = 
                new com.truthlens.search.model.DuckDuckGoApiResponse.RelatedTopic("  ", "url", null, null, null);
        com.truthlens.search.model.DuckDuckGoApiResponse.RelatedTopic nestedTopicValid = 
                new com.truthlens.search.model.DuckDuckGoApiResponse.RelatedTopic("valid", "validUrl", null, null, null);
        com.truthlens.search.model.DuckDuckGoApiResponse.RelatedTopic topicWithTopics = 
                new com.truthlens.search.model.DuckDuckGoApiResponse.RelatedTopic(null, null, null, List.of(nestedTopicBlank, nestedTopicValid), null);
        
        com.truthlens.search.model.DuckDuckGoApiResponse ddgTopics = new com.truthlens.search.model.DuckDuckGoApiResponse(
                null, null, null, null, null, null, null, null, null, null, List.of(blankTopic, topicWithTopics)
        );
        when(responseSpec.body(com.truthlens.search.model.DuckDuckGoApiResponse.class)).thenReturn(ddgTopics);
        var resTopics = webSearchService.executeSearch(new SearchExecutionRequest(List.of("query"), 3)).results();
        assertThat(resTopics).hasSize(1);
        assertThat(resTopics.get(0).title()).isEqualTo("valid");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDuckDuckGoBranchCoverage_TopicsBreak() {
        setupMockClients();
        com.truthlens.search.model.DuckDuckGoApiResponse.RelatedTopic valid1 = 
                new com.truthlens.search.model.DuckDuckGoApiResponse.RelatedTopic("t1", "u1", null, null, null);
        com.truthlens.search.model.DuckDuckGoApiResponse.RelatedTopic valid2 = 
                new com.truthlens.search.model.DuckDuckGoApiResponse.RelatedTopic("t2", "u2", null, null, null);
        com.truthlens.search.model.DuckDuckGoApiResponse.RelatedTopic topicWithTopics2 = 
                new com.truthlens.search.model.DuckDuckGoApiResponse.RelatedTopic(null, null, null, List.of(valid1, valid2), null);

        com.truthlens.search.model.DuckDuckGoApiResponse ddgTopicsBreak = new com.truthlens.search.model.DuckDuckGoApiResponse(
                null, null, null, null, null, null, null, null, null, null, List.of(valid1, valid2, topicWithTopics2)
        );
        when(responseSpec.body(com.truthlens.search.model.DuckDuckGoApiResponse.class)).thenReturn(ddgTopicsBreak);
        var resTopicsBreak = webSearchService.executeSearch(new SearchExecutionRequest(List.of("query"), 1)).results();
        assertThat(resTopicsBreak).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDuckDuckGoBranchCoverage_NestedBreak() {
        setupMockClients();
        com.truthlens.search.model.DuckDuckGoApiResponse.RelatedTopic valid1 = 
                new com.truthlens.search.model.DuckDuckGoApiResponse.RelatedTopic("t1", "u1", null, null, null);
        com.truthlens.search.model.DuckDuckGoApiResponse.RelatedTopic valid2 = 
                new com.truthlens.search.model.DuckDuckGoApiResponse.RelatedTopic("t2", "u2", null, null, null);
        com.truthlens.search.model.DuckDuckGoApiResponse.RelatedTopic topicWithTopics2 = 
                new com.truthlens.search.model.DuckDuckGoApiResponse.RelatedTopic(null, null, null, List.of(valid1, valid2), null);

        when(responseSpec.body(com.truthlens.search.model.DuckDuckGoApiResponse.class)).thenReturn(
                new com.truthlens.search.model.DuckDuckGoApiResponse(null, null, null, null, null, null, null, null, null, null, List.of(topicWithTopics2))
        );
        var resNestedBreak = webSearchService.executeSearch(new SearchExecutionRequest(List.of("query"), 1)).results();
        assertThat(resNestedBreak).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testExtractTitleNull() throws Exception {
        java.lang.reflect.Method method = WebSearchService.class.getDeclaredMethod("extractTitle", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(webSearchService, (String) null);
        assertThat(result).isEqualTo("Search Result");
        
        String longText = "This is a very long text that will exceed the sixty characters limit easily and then get truncated.";
        String longResult = (String) method.invoke(webSearchService, longText);
        assertThat(longResult).endsWith("...");
        
        String hyphenText = "Prefix - Suffix";
        String hyphenResult = (String) method.invoke(webSearchService, hyphenText);
        assertThat(hyphenResult).isEqualTo("Prefix");
        
        String hyphenLateText = "This prefix is extremely long and will go past sixty characters before it reaches the - dash";
        String hyphenLateResult = (String) method.invoke(webSearchService, hyphenLateText);
        assertThat(hyphenLateResult).endsWith("...");
    }
}
