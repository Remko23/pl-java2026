package com.truthlens.backend.controller;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SystemHealthController {

    private final DiscoveryClient discoveryClient;

    public SystemHealthController(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @GetMapping("/api/v1/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        boolean ocrUp = !discoveryClient.getInstances("truthlens-ocr-service").isEmpty();
        boolean searchUp = !discoveryClient.getInstances("truthlens-search-service").isEmpty();
        
        boolean allUp = ocrUp && searchUp;
        
        return ResponseEntity.ok(Map.of(
            "status", allUp ? "UP" : "DOWN",
            "ocr", ocrUp ? "UP" : "DOWN",
            "search", searchUp ? "UP" : "DOWN"
        ));
    }
}
