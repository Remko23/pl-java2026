package com.truthlens.backend.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truthlens.backend.client.GroqApiClient;
import com.truthlens.backend.client.OcrServiceClient;
import com.truthlens.backend.client.SearchServiceClient;
import com.truthlens.backend.model.*;
import com.truthlens.backend.model.jury.GroqChatResponse;
import com.truthlens.backend.model.jury.GroqMessage;
import com.truthlens.backend.repository.VerificationHistoryRepository;
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

/**
 * Cucumber Step Definitions for the TruthLens fact-checking verification flow.
 * <p>
 * This class covers the full end-to-end BDD scenario:
 * <ol>
 *   <li>Authenticating via a mocked JWT token</li>
 *   <li>Stubbing external services (Search, Groq LLM / AI Jury)</li>
 *   <li>Submitting a verification request via {@code POST /api/v1/verifications}</li>
 *   <li>Polling for asynchronous completion via {@code GET /api/v1/verifications/{id}}</li>
 *   <li>Asserting the final verdict, status, and AI reasoning</li>
 * </ol>
 * <p>
 * Architecture note: The orchestrator processes verifications asynchronously on
 * virtual threads. The POST endpoint returns immediately with 202 / QUEUED.
 * The step definitions therefore poll the GET endpoint until the status reaches
 * COMPLETED (or a timeout fires).
 * <p>
 * All {@code @MockBean} annotations live on {@link CucumberSpringConfiguration}
 * (the {@code @CucumberContextConfiguration} class). The mock instances are
 * injected here via {@code @Autowired} to configure stubs per scenario.
 */
public class VerificationStepDefinitions {

    // ──────────────────────────────────────────────────────────────────────
    // Injected Spring beans
    // ──────────────────────────────────────────────────────────────────────

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ──────────────────────────────────────────────────────────────────────
    // Mocked external dependencies (beans created by @MockBean on
    // CucumberSpringConfiguration, injected here for stub configuration)
    // ──────────────────────────────────────────────────────────────────────

    @Autowired
    private SearchServiceClient searchServiceClient;

    @Autowired
    private GroqApiClient groqApiClient;

    // ──────────────────────────────────────────────────────────────────────
    // Scenario-scoped state (reset per scenario by Cucumber)
    // ──────────────────────────────────────────────────────────────────────

    /** Stores the jury vote stub data parsed from the Gherkin DataTable. */
    private List<JuryVoteStub> juryVoteStubs = new ArrayList<>();

    /** The MvcResult from the POST submission (contains verificationId). */
    private MvcResult submitResult;

    /** The parsed final response after polling reaches COMPLETED. */
    private JsonNode finalResponse;

    /** The verification ID extracted from the initial POST response. */
    private String verificationId;

    /** HTTP status code of the initial POST submission. */
    private int submitStatusCode;

    // ──────────────────────────────────────────────────────────────────────
    // Inner helper record for DataTable mapping
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Maps a single row from the Cucumber DataTable to a strongly typed object.
     */
    private record JuryVoteStub(String model, String vote, int confidence) {
    }

    // ──────────────────────────────────────────────────────────────────────
    // GIVEN steps
    // ──────────────────────────────────────────────────────────────────────

    @Given("the user is authenticated")
    public void theUserIsAuthenticated() {
        // Authentication is handled per-request using SecurityMockMvcRequestPostProcessors.jwt()
        // in the @When step. No additional setup is needed here — the mock JwtDecoder
        // ensures Spring Security accepts the synthetic JWT without calling Keycloak.
    }

    @And("the external search service will return {int} web results contradicting the claim")
    public void theExternalSearchServiceWillReturnWebResults(int resultCount) {
        List<SearchResult> searchResults = new ArrayList<>();
        for (int i = 1; i <= resultCount; i++) {
            searchResults.add(new SearchResult(
                    "Contradicting Evidence " + i,
                    "https://science.example.com/evidence-" + i,
                    "Scientific evidence #" + i + " clearly contradicts the claim. "
                            + "Multiple peer-reviewed studies confirm this is false."
            ));
        }
        when(searchServiceClient.executeSearch(any(SearchExecutionRequest.class)))
                .thenReturn(new SearchExecutionResponse(searchResults));
    }

    @And("the AI jury models will vote as follows:")
    public void theAiJuryModelsWillVoteAsFollows(DataTable dataTable) {
        // Parse the DataTable rows (skipping the header) into JuryVoteStub records
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        juryVoteStubs.clear();
        for (Map<String, String> row : rows) {
            juryVoteStubs.add(new JuryVoteStub(
                    row.get("Model"),
                    row.get("Vote"),
                    Integer.parseInt(row.get("Confidence"))
            ));
        }

        // The GroqLlmService.askModel() calls GroqApiClient.getChatCompletion() internally.
        // We stub the GroqApiClient at the HTTP interface level so all layers above
        // (GroqLlmService → JuryVotingService → Orchestrator) work with real logic.
        //
        // Call sequence from orchestrator:
        //   1st call → askModel("llama-3.1-8b-instant", generateQueriesPrompt)  → returns query JSON
        //   2nd..4th calls → askModel(juryModel, votingPrompt)                  → returns verdict JSON
        //
        // We use a sequential answer chain: first answer handles query generation,
        // subsequent answers handle the jury votes for each model.

        // Build the query-generation response (first call to askModel)
        String queryJson = "[\"flat earth debunked\", \"earth shape scientific evidence\", \"flat earth conspiracy\"]";
        GroqChatResponse queryResponse = buildGroqChatResponse(queryJson);

        // Build jury vote responses (one per model in the DataTable)
        List<GroqChatResponse> juryResponses = new ArrayList<>();
        for (JuryVoteStub stub : juryVoteStubs) {
            // Map the Gherkin "FALSE" vote → a numeric verdict ≤50 (as expected by the orchestrator logic)
            int numericVerdict = "TRUE".equalsIgnoreCase(stub.vote()) ? 85 : 15;
            String voteJson = """
                    {"verdict": %d, "confidenceScore": %d, "reasoning": "%s model analysis: The claim has been evaluated against available evidence and determined to be %s with %d%% confidence."}
                    """.formatted(numericVerdict, stub.confidence(), stub.model(), stub.vote(), stub.confidence());
            juryResponses.add(buildGroqChatResponse(voteJson));
        }

        // Chain the responses: first the query generation, then jury votes in order
        var stubbing = when(groqApiClient.getChatCompletion(anyString(), any()))
                .thenReturn(queryResponse);
        for (GroqChatResponse juryResponse : juryResponses) {
            stubbing = stubbing.thenReturn(juryResponse);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // WHEN steps
    // ──────────────────────────────────────────────────────────────────────

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

        // Extract the verificationId from the response for subsequent polling
        JsonNode responseNode = objectMapper.readTree(submitResult.getResponse().getContentAsString());
        verificationId = responseNode.get("verificationId").asText();
    }

    // ──────────────────────────────────────────────────────────────────────
    // THEN steps
    // ──────────────────────────────────────────────────────────────────────

    @Then("the response status should be {int} ACCEPTED")
    public void theResponseStatusShouldBeAccepted(int expectedStatus) {
        assertThat(submitStatusCode)
                .as("HTTP status code of the initial POST submission")
                .isEqualTo(expectedStatus);
    }

    @And("the verification status should eventually be {string}")
    public void theVerificationStatusShouldEventuallyBe(String expectedStatus) {
        // The orchestrator processes the verification asynchronously on a virtual thread.
        // We poll the GET endpoint until the status transitions to the expected value.
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

                    // Store the final response for subsequent assertions
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

        // Verify that reasoning contains references to each jury model from the DataTable
        for (JuryVoteStub stub : juryVoteStubs) {
            assertThat(reasoning)
                    .as("Reasoning should contain analysis from model: " + stub.model())
                    .containsIgnoringCase(stub.model());
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Helper methods
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Builds a {@link GroqChatResponse} wrapping the given content string,
     * mimicking the Groq API's {@code choices[0].message.content} structure.
     */
    private GroqChatResponse buildGroqChatResponse(String content) {
        GroqMessage message = new GroqMessage("assistant", content);
        GroqChatResponse.Choice choice = new GroqChatResponse.Choice(message);
        return new GroqChatResponse(List.of(choice));
    }
}
