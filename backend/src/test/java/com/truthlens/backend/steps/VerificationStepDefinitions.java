package com.truthlens.backend.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truthlens.backend.client.GroqApiClient;
import com.truthlens.backend.client.SearchServiceClient;
import com.truthlens.backend.model.*;
import com.truthlens.backend.model.jury.GroqChatResponse;
import com.truthlens.backend.model.jury.GroqMessage;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class VerificationStepDefinitions {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SearchServiceClient searchServiceClient;

    @Autowired
    private GroqApiClient groqApiClient;

    private List<JuryVoteStub> juryVoteStubs = new ArrayList<>();
    private MvcResult submitResult;
    private JsonNode finalResponse;
    private String verificationId;
    private int submitStatusCode;

    private record JuryVoteStub(String model, String vote, int confidence) {
    }

    @Given("the user is authenticated")
    public void theUserIsAuthenticated() {
        // puste
    }

    @And("the external search service will return {int} web results contradicting the claim")
    public void theExternalSearchServiceWillReturnWebResults(int resultCount) {
        List<SearchResult> searchResults = new ArrayList<>();
        for (int i = 1; i <= resultCount; i++) {
            searchResults.add(new SearchResult(
                    "Contradicting Evidence " + i,
                    "https://science.example.com/evidence-" + i,
                    "Scientific evidence #" + i + " clearly contradicts the claim. "
                            + "Multiple peer-reviewed studies confirm this is false."));
        }
        when(searchServiceClient.executeSearch(any(SearchExecutionRequest.class)))
                .thenReturn(new SearchExecutionResponse(searchResults));
    }

    @And("the AI jury models will vote as follows:")
    public void theAiJuryModelsWillVoteAsFollows(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        juryVoteStubs.clear();
        for (Map<String, String> row : rows) {
            juryVoteStubs.add(new JuryVoteStub(
                    row.get("Model"),
                    row.get("Vote"),
                    Integer.parseInt(row.get("Confidence"))));
        }

        String queryJson = "[\"flat earth debunked\", \"earth shape scientific evidence\", \"flat earth conspiracy\"]";
        GroqChatResponse queryResponse = buildGroqChatResponse(queryJson);

        List<GroqChatResponse> juryResponses = new ArrayList<>();
        for (JuryVoteStub stub : juryVoteStubs) {
            int numericVerdict = "TRUE".equalsIgnoreCase(stub.vote()) ? 85 : 15;
            String voteJson = """
                    {"verdict": %d, "confidenceScore": %d, "reasoning": "%s model analysis: The claim has been evaluated against available evidence and determined to be %s with %d%% confidence."}
                    """
                    .formatted(numericVerdict, stub.confidence(), stub.model(), stub.vote(), stub.confidence());
            juryResponses.add(buildGroqChatResponse(voteJson));
        }

        var stubbing = when(groqApiClient.getChatCompletion(anyString(), any()))
                .thenReturn(queryResponse);
        for (GroqChatResponse juryResponse : juryResponses) {
            stubbing = stubbing.thenReturn(juryResponse);
        }
    }

    @When("the user submits a verification request with the text {string}")
    public void theUserSubmitsAVerificationRequestWithText(String claimText) throws Exception {
        String requestJson = objectMapper.writeValueAsString(new VerificationRequest(claimText));

        submitResult = mockMvc.perform(post("/api/v1/verifications")
                .with(jwt().jwt(builder -> builder
                        .subject("test-user-id")
                        .claim("preferred_username", "testuser")
                        .claim("email", "test@truthlens.com")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andReturn();

        submitStatusCode = submitResult.getResponse().getStatus();

        JsonNode responseNode = objectMapper.readTree(submitResult.getResponse().getContentAsString());
        verificationId = responseNode.get("verificationId").asText();
    }

    @Then("the response status should be {int} ACCEPTED")
    public void theResponseStatusShouldBeAccepted(int expectedStatus) {
        assertThat(submitStatusCode)
                .as("HTTP status code of the initial POST submission")
                .isEqualTo(expectedStatus);
    }

    @And("the verification status should eventually be {string}")
    public void theVerificationStatusShouldEventuallyBe(String expectedStatus) {
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(250, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    MvcResult pollResult = mockMvc.perform(get("/api/v1/verifications/" + verificationId))
                            .andExpect(status().isOk())
                            .andReturn();

                    JsonNode node = objectMapper.readTree(pollResult.getResponse().getContentAsString());
                    String currentStatus = node.get("status").asText();
                    assertThat(currentStatus)
                            .as("Verification status after async processing")
                            .isEqualTo(expectedStatus);

                    finalResponse = node;
                });
    }

    @And("the final verification verdict should be {string}")
    public void theFinalVerificationVerdictShouldBe(String expectedVerdict) {
        assertThat(finalResponse).as("Final response must be available (poll must have succeeded)").isNotNull();
        assertThat(finalResponse.has("result")).as("Response must contain a 'result' field").isTrue();

        JsonNode result = finalResponse.get("result");
        assertThat(result.has("finalVerdict")).as("Result must contain 'finalVerdict'").isTrue();
        assertThat(result.get("finalVerdict").asText())
                .as("The AI jury's final verdict")
                .isEqualTo(expectedVerdict);
    }

    @And("the response should contain the reasoning from the AI models")
    public void theResponseShouldContainTheReasoningFromTheAiModels() {
        assertThat(finalResponse).as("Final response must be available").isNotNull();

        JsonNode result = finalResponse.get("result");
        assertThat(result.has("aggregatedReasoning")).as("Result must contain 'aggregatedReasoning'").isTrue();

        String reasoning = result.get("aggregatedReasoning").asText();
        assertThat(reasoning)
                .as("Aggregated reasoning should not be empty")
                .isNotBlank();

        for (JuryVoteStub stub : juryVoteStubs) {
            assertThat(reasoning)
                    .as("Reasoning should contain analysis from model: " + stub.model())
                    .containsIgnoringCase(stub.model());
        }
    }

    private GroqChatResponse buildGroqChatResponse(String content) {
        GroqMessage message = new GroqMessage("assistant", content);
        GroqChatResponse.Choice choice = new GroqChatResponse.Choice(message);
        return new GroqChatResponse(List.of(choice));
    }
}
