package com.truthlens.search.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WikipediaApiResponseTest {

    @Test
    void testWikipediaApiResponseRecords() {
        WikipediaApiResponse.SearchItem item1 = new WikipediaApiResponse.SearchItem("title1", "snippet1");
        WikipediaApiResponse.SearchItem item2 = new WikipediaApiResponse.SearchItem("title1", "snippet1");

        assertThat(item1.title()).isEqualTo("title1");
        assertThat(item1.snippet()).isEqualTo("snippet1");
        assertThat(item1).isEqualTo(item2);
        assertThat(item1.hashCode()).isEqualTo(item2.hashCode());
        assertThat(item1.toString()).contains("title1");

        WikipediaApiResponse.Query query1 = new WikipediaApiResponse.Query(List.of(item1));
        WikipediaApiResponse.Query query2 = new WikipediaApiResponse.Query(List.of(item2));

        assertThat(query1.search()).hasSize(1);
        assertThat(query1).isEqualTo(query2);
        assertThat(query1.hashCode()).isEqualTo(query2.hashCode());
        assertThat(query1.toString()).contains("search");

        WikipediaApiResponse response1 = new WikipediaApiResponse(query1);
        WikipediaApiResponse response2 = new WikipediaApiResponse(query2);

        assertThat(response1.query()).isEqualTo(query1);
        assertThat(response1).isEqualTo(response2);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        assertThat(response1.toString()).contains("query");
    }
}
