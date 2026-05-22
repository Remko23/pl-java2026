package com.truthlens.search.model;

import java.util.List;

public record WikipediaApiResponse(Query query) {
    public record Query(List<SearchItem> search) {}
    public record SearchItem(String title, String snippet) {}
}
