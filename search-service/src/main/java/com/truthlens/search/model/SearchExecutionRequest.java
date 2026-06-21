package com.truthlens.search.model;

import java.util.List;

public record SearchExecutionRequest(
        List<String> queries,
        int maxResultsPerQuery
) {
}
