package com.truthlens.backend.client;

import com.truthlens.backend.model.SearchExecutionRequest;
import com.truthlens.backend.model.SearchExecutionResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface SearchServiceClient {

    @PostExchange("/api/internal/v1/search:execute")
    SearchExecutionResponse executeSearch(@RequestBody SearchExecutionRequest request);
}
