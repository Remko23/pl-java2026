package com.truthlens.gateway.steps;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.stream.IntStream;

public class GatewayStepDefinitions {

    @Autowired
    private WebTestClient webTestClient;

    private WebTestClient.ResponseSpec lastResponse;

    @Given("an anonymous user")
    public void an_anonymous_user() {
        // No authentication token added to requests
    }

    @When("the user sends {int} requests to {string}")
    public void the_user_sends_requests(int count, String path) {
        IntStream.range(0, count).forEach(i -> {
            lastResponse = webTestClient.get().uri(java.util.Objects.requireNonNull(path)).exchange();
        });
    }

    @Then("the {int}th request should be blocked with {int} Too Many Requests")
    public void the_last_request_should_be_blocked(int index, int expectedStatus) {
        // We assert on the last response which is the 11th request
        lastResponse.expectStatus().isEqualTo(expectedStatus);
    }

    @Given("a preflight OPTIONS request to {string} from {string}")
    public void a_preflight_options_request(String path, String origin) {
        lastResponse = webTestClient.options()
                .uri(java.util.Objects.requireNonNull(path))
                .header("Origin", java.util.Objects.requireNonNull(origin))
                .header("Access-Control-Request-Method", "GET")
                .exchange();
    }

    @When("the request is processed")
    public void the_request_is_processed() {
        // Processing occurs simultaneously in the Given step (exchange())
    }

    @Then("it should return {int} OK")
    public void it_should_return_status(int status) {
        lastResponse.expectStatus().isEqualTo(status);
    }

    @And("the response should contain header {string} with value {string}")
    public void the_response_should_contain_header(String header, String value) {
        lastResponse.expectHeader().valueEquals(
                java.util.Objects.requireNonNull(header), 
                java.util.Objects.requireNonNull(value)
        );
    }
}
