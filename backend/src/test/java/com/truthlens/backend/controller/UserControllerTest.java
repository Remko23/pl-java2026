package com.truthlens.backend.controller;

import com.truthlens.backend.model.User;
import com.truthlens.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import com.truthlens.backend.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class, properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false"})
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void testGetStatus() throws Exception {
        mockMvc.perform(get("/api/public/status"))
                .andExpect(status().isOk())
                .andExpect(content().string("TruthLens Backend is running and connected to DB (tested implicitly on startup)"));
    }

    @Test
    void testGetCurrentUser_ExistingUser() throws Exception {
        User user = new User("sub-123", "test@example.com", "John", "Doe");
        when(userRepository.findByKeycloakId("sub-123")).thenReturn(Optional.of(user));

        Jwt token = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "sub-123")
                .claim("email", "test@example.com")
                .claim("given_name", "John")
                .claim("family_name", "Doe")
                .build();

        mockMvc.perform(get("/api/users/me").with(jwt().jwt(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keycloakId").value("sub-123"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void testGetCurrentUser_NewUser() throws Exception {
        when(userRepository.findByKeycloakId("sub-new")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        Jwt token = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "sub-new")
                .claim("email", "new@example.com")
                .build();

        mockMvc.perform(get("/api/users/me").with(jwt().jwt(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keycloakId").value("sub-new"))
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }
}
