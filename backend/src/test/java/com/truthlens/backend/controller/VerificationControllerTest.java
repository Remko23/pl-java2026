package com.truthlens.backend.controller;

import com.truthlens.backend.model.VerificationResponse;
import com.truthlens.backend.model.VerificationStatus;
import com.truthlens.backend.service.VerificationOrchestratorService;
import com.truthlens.backend.service.VerificationStateService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import com.truthlens.backend.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = VerificationController.class, properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false"})
@Import(SecurityConfig.class)
class VerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VerificationOrchestratorService orchestratorService;

    @MockitoBean
    private VerificationStateService stateService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void testVerifyText() throws Exception {
        VerificationResponse mockResponse = new VerificationResponse("test-id", VerificationStatus.QUEUED, 0, "Queued", null);
        when(orchestratorService.startVerification(anyString(), any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/verifications")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"claimText\": \"test claim\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.verificationId").value("test-id"))
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    void testVerifyImage() throws Exception {
        VerificationResponse mockResponse = new VerificationResponse("test-id-img", VerificationStatus.QUEUED, 0, "Queued", null);
        when(orchestratorService.startVerification(any(org.springframework.web.multipart.MultipartFile.class), any())).thenReturn(mockResponse);

        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "test image content".getBytes());

        mockMvc.perform(multipart("/api/v1/verifications")
                .file(file)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.verificationId").value("test-id-img"));
    }

    @Test
    void testGetStatus_Found() throws Exception {
        VerificationResponse mockResponse = new VerificationResponse("test-id", VerificationStatus.COMPLETED, 100, "Done", null);
        when(stateService.getState("test-id")).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/verifications/test-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationId").value("test-id"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void testGetStatus_NotFound() throws Exception {
        when(stateService.getState("unknown")).thenReturn(null);

        mockMvc.perform(get("/api/v1/verifications/unknown"))
                .andExpect(status().isNotFound());
    }
}
