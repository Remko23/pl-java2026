package com.truthlens.backend.model;

import java.util.List;

public record SearchExecutionRequest(List<String> queries, int maxResultsPerQuery) {
}
