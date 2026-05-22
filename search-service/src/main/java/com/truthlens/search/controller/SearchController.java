package com.truthlens.search.controller;

import com.truthlens.search.model.SearchExecutionRequest;
import com.truthlens.search.model.SearchExecutionResponse;
import com.truthlens.search.service.WebSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/v1")
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final WebSearchService webSearchService;

    public SearchController(WebSearchService webSearchService) {
        this.webSearchService = webSearchService;
    }

    @PostMapping("/search:execute")
    public ResponseEntity<SearchExecutionResponse> executeSearch(
            @RequestBody SearchExecutionRequest request) {

        log.info("Received search request with {} queries, maxResultsPerQuery={}",
                request.queries() != null ? request.queries().size() : 0,
                request.maxResultsPerQuery());

        SearchExecutionResponse response = webSearchService.executeSearch(request);

        log.info("Returning {} search results", response.results().size());

        return ResponseEntity.ok(response);
    }
}
