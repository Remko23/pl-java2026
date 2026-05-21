Feature: API Gateway Security and Routing

  Scenario: Anonymous user can access the verifications endpoint
    Given an anonymous user
    When the user sends a GET request to "/api/v1/verifications"
    Then the response should NOT be 401 Unauthorized

  Scenario: Rate limiting blocks requests from anonymous user exceeding burst capacity
    Given an anonymous user with a unique IP "10.0.0.1"
    When the user sends 5 sequential requests to "/api/v1/verifications"
    Then at least one response should have status 429 Too Many Requests

  Scenario: Valid CORS preflight request is accepted
    Given a preflight OPTIONS request to "/api/v1/verifications" from "http://localhost:3000"
    When the request is processed
    Then it should return 200 OK
    And the response should contain header "Access-Control-Allow-Origin" with value "http://localhost:3000"

  Scenario: CORS preflight from unauthorized origin is rejected
    Given a preflight OPTIONS request to "/api/v1/verifications" from "http://evil.com"
    When the request is processed
    Then the response should NOT contain header "Access-Control-Allow-Origin" with value "http://evil.com"
