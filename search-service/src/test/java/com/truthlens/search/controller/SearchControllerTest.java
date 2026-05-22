package com.truthlens.search.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truthlens.search.model.SearchExecutionRequest;
import com.truthlens.search.model.SearchExecutionResponse;
import com.truthlens.search.model.SearchResultItem;
import com.truthlens.search.service.WebSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SearchController.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WebSearchService webSearchService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/internal/v1/search:execute — should return 200 with results")
    void shouldReturnResults() throws Exception {
        var expectedResults = List.of(
                new SearchResultItem(
                        "Test Article",
                        "https://example.com/article",
                        "This is a test article snippet"
                )
        );

        when(webSearchService.executeSearch(any(SearchExecutionRequest.class)))
                .thenReturn(new SearchExecutionResponse(expectedResults));

        var requestBody = new SearchExecutionRequest(
                List.of("test query"),
                3
        );

        mockMvc.perform(post("/api/internal/v1/search:execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results.length()").value(1))
                .andExpect(jsonPath("$.results[0].title").value("Test Article"))
                .andExpect(jsonPath("$.results[0].url").value("https://example.com/article"))
                .andExpect(jsonPath("$.results[0].snippet").value("This is a test article snippet"));
    }

    @Test
    @DisplayName("POST /api/internal/v1/search:execute — should return 200 with empty results")
    void shouldReturnEmptyResults() throws Exception {
        when(webSearchService.executeSearch(any(SearchExecutionRequest.class)))
                .thenReturn(new SearchExecutionResponse(List.of()));

        var requestBody = new SearchExecutionRequest(
                List.of("obscure query"),
                3
        );

        mockMvc.perform(post("/api/internal/v1/search:execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results.length()").value(0));
    }

    @Test
    @DisplayName("POST /api/internal/v1/search:execute — should return 400 on bad request")
    void shouldReturn400OnBadRequest() throws Exception {
        when(webSearchService.executeSearch(any(SearchExecutionRequest.class)))
                .thenThrow(new IllegalArgumentException("At least one search query is required"));

        var requestBody = new SearchExecutionRequest(List.of(), 3);

        mockMvc.perform(post("/api/internal/v1/search:execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("At least one search query is required"));
    }

    @Test
    @DisplayName("POST /api/internal/v1/search:execute — should return 503 on external search failure")
    void shouldReturn503OnExternalSearchFailure() throws Exception {
        when(webSearchService.executeSearch(any(SearchExecutionRequest.class)))
                .thenThrow(new com.truthlens.search.exception.ExternalSearchException(
                        "DuckDuckGo rate limit exceeded (429)"));

        var requestBody = new SearchExecutionRequest(List.of("query"), 3);

        mockMvc.perform(post("/api/internal/v1/search:execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("EXTERNAL_SEARCH_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("POST /api/internal/v1/search:execute — should return multiple results in correct JSON format")
    void shouldReturnMultipleResultsInCorrectFormat() throws Exception {
        var results = List.of(
                new SearchResultItem("Article 1", "https://example.com/1", "Snippet 1"),
                new SearchResultItem("Article 2", "https://example.com/2", "Snippet 2"),
                new SearchResultItem("Article 3", "https://example.com/3", "Snippet 3")
        );

        when(webSearchService.executeSearch(any(SearchExecutionRequest.class)))
                .thenReturn(new SearchExecutionResponse(results));

        var requestBody = new SearchExecutionRequest(List.of("query 1", "query 2"), 3);

        mockMvc.perform(post("/api/internal/v1/search:execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(3))
                .andExpect(jsonPath("$.results[0].title").value("Article 1"))
                .andExpect(jsonPath("$.results[1].title").value("Article 2"))
                .andExpect(jsonPath("$.results[2].title").value("Article 3"))
                .andExpect(jsonPath("$.results[0].url").value("https://example.com/1"))
                .andExpect(jsonPath("$.results[1].snippet").value("Snippet 2"));
    }
}
