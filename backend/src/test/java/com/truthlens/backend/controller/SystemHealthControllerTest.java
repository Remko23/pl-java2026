package com.truthlens.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.test.web.servlet.MockMvc;
import com.truthlens.backend.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SystemHealthController.class, properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false"})
@Import(SecurityConfig.class)
class SystemHealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DiscoveryClient discoveryClient;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void testGetHealth_AllUp() throws Exception {
        when(discoveryClient.getInstances("truthlens-ocr-service")).thenReturn(List.of(mock(ServiceInstance.class)));
        when(discoveryClient.getInstances("truthlens-search-service")).thenReturn(List.of(mock(ServiceInstance.class)));

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.ocr").value("UP"))
                .andExpect(jsonPath("$.search").value("UP"));
    }

    @Test
    void testGetHealth_Down() throws Exception {
        when(discoveryClient.getInstances("truthlens-ocr-service")).thenReturn(List.of());
        when(discoveryClient.getInstances("truthlens-search-service")).thenReturn(List.of(mock(ServiceInstance.class)));

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.ocr").value("DOWN"))
                .andExpect(jsonPath("$.search").value("UP"));
    }
}
