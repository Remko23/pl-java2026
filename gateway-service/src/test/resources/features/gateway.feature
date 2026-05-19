Feature: API Gateway Routing and Rate Limiting

  Scenario: Rate limiting blocks requests after burst capacity
    Given an anonymous user
    When the user sends 11 requests to "/api/v1/verifications"
    Then the 11th request should be blocked with 429 Too Many Requests

  Scenario: Valid CORS Preflight Request
    Given a preflight OPTIONS request to "/api/v1/verifications" from "http://localhost:3000"
    When the request is processed
    Then it should return 200 OK
    And the response should contain header "Access-Control-Allow-Origin" with value "http://localhost:3000"
