package com.truthlens.search.model;

import java.util.List;

public record DuckDuckGoApiResponse(
        String Abstract,
        String AbstractText,
        String AbstractSource,
        String AbstractURL,
        String Heading,
        String Answer,
        String AnswerType,
        String Definition,
        String DefinitionSource,
        String DefinitionURL,
        List<RelatedTopic> RelatedTopics
) {
    public record RelatedTopic(
            String Text,
            String FirstURL,
            String Result,
            List<RelatedTopic> Topics,
            String Name
    ) {}
}
