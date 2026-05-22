package com.truthlens.search.model;

import java.util.List;

/**
 * Request DTO for executing a web search.
 * Contract from DECISION_LOG_API.md §4.3:
 *   { "queries": ["..."], "maxResultsPerQuery": 3 }
 */
public record SearchExecutionRequest(
        List<String> queries,
        int maxResultsPerQuery
) {
}
