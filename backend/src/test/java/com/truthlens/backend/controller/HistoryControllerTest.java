package com.truthlens.backend.controller;

import com.truthlens.backend.model.VerificationHistory;
import com.truthlens.backend.service.VerificationHistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import com.truthlens.backend.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = HistoryController.class, properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false"})
@Import(SecurityConfig.class)
class HistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VerificationHistoryService historyService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void testGetHistory() throws Exception {
        VerificationHistory history = new VerificationHistory();
        history.setId("hist-id");
        history.setInputText("test text");
        Page<VerificationHistory> page = new PageImpl<>(List.of(history));

        when(historyService.getHistory(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/history?page=0&size=10")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("hist-id"))
                .andExpect(jsonPath("$.content[0].inputText").value("test text"));
    }
}
