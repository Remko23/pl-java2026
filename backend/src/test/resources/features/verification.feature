Feature: Fact-checking orchestration
  The TruthLens backend processes verification requests by searching the web
  for evidence, then letting an AI jury of multiple LLM models vote on the
  truthfulness of the claim.

  Scenario: A user submits a claim as text and receives a FALSE verdict
    Given the user is authenticated
    And the external search service will return 3 web results contradicting the claim
    And the AI jury models will vote as follows:
      | Model     | Vote  | Confidence |
      | Llama-8B  | FALSE | 90         |
      | Llama-70B | FALSE | 95         |
      | Gemma     | FALSE | 80         |
    When the user submits a verification request with the text "The Earth is flat"
    Then the response status should be 202 ACCEPTED
    And the verification status should eventually be "COMPLETED"
    And the final verification verdict should be "FALSE"
    And the response should contain the reasoning from the AI models
