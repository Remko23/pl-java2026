package com.truthlens.gateway.steps;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class GatewayStepDefinitions {

    @Autowired
    private WebTestClient webTestClient;

    private WebTestClient.ResponseSpec lastResponse;
    private final List<Integer> collectedStatuses = new ArrayList<>();

    @Given("an anonymous user")
    public void an_anonymous_user() {
        collectedStatuses.clear();
    }

    @Given("an anonymous user with a unique IP {string}")
    public void an_anonymous_user_with_unique_ip(String ip) {
        collectedStatuses.clear();
    }

    @Given("a preflight OPTIONS request to {string} from {string}")
    public void a_preflight_options_request(String path, String origin) {
        collectedStatuses.clear();
        lastResponse = webTestClient.options()
                .uri(Objects.requireNonNull(path))
                .header("Origin", Objects.requireNonNull(origin))
                .header("Access-Control-Request-Method", "GET")
                .exchange();
    }

    @When("the user sends a GET request to {string}")
    public void the_user_sends_single_request(String path) {
        lastResponse = webTestClient.get()
                .uri(Objects.requireNonNull(path))
                .exchange();
    }

    @When("the user sends {int} sequential requests to {string}")
    public void the_user_sends_sequential_requests(int count, String path) {
        collectedStatuses.clear();
        for (int i = 0; i < count; i++) {
            int status = webTestClient.get()
                    .uri(Objects.requireNonNull(path))
                    .exchange()
                    .returnResult(String.class)
                    .getStatus()
                    .value();
            collectedStatuses.add(status);
        }
    }

    @When("the request is processed")
    public void the_request_is_processed() {
    }
    @Then("the response should NOT be {int} Unauthorized")
    public void the_response_should_not_be_unauthorized(int status) {
        int actualStatus = lastResponse.returnResult(String.class).getStatus().value();
        assertThat(actualStatus).isNotEqualTo(status);
    }

    @Then("at least one response should have status {int} Too Many Requests")
    public void at_least_one_response_should_be_429(int expectedStatus) {
        assertThat(collectedStatuses)
                .as("Expected at least one HTTP %d among responses: %s", expectedStatus, collectedStatuses)
                .contains(expectedStatus);
    }

    @Then("it should return {int} OK")
    public void it_should_return_status(int status) {
        lastResponse.expectStatus().isEqualTo(status);
    }

    @And("the response should contain header {string} with value {string}")
    public void the_response_should_contain_header(String header, String value) {
        lastResponse.expectHeader().valueEquals(
                Objects.requireNonNull(header),
                Objects.requireNonNull(value)
        );
    }

    @Then("the response should NOT contain header {string} with value {string}")
    public void the_response_should_not_contain_header_with_value(String header, String value) {
        var headers = lastResponse.returnResult(String.class).getResponseHeaders();
        var headerValues = headers.get(Objects.requireNonNull(header));

        if (headerValues != null) {
            assertThat(headerValues)
                    .as("Header '%s' should not contain forbidden value '%s'", header, value)
                    .doesNotContain(Objects.requireNonNull(value));
        }
    }
}

