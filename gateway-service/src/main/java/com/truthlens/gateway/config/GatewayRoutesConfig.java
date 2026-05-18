package com.truthlens.gateway.config;

// TODO: Implement programmatic route definitions (or use application.yml).
// Required routes from DECISION_LOG_API.md §4.1:
//   - POST /api/v1/verifications       -> lb://truthlens-backend
//   - GET  /api/v1/verifications/{id}   -> lb://truthlens-backend
//   - GET  /api/auth/**                 -> lb://truthlens-keycloak
// All internal services (search-service, ocr-service) must NOT be routable from here.

public class GatewayRoutesConfig {
}
