package com.truthlens.search.model;

import java.util.List;


public record SearchExecutionResponse(
        List<SearchResultItem> results
) {
}
