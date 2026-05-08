package com.truthlens.backend.controller;

import com.truthlens.backend.model.User;
import com.truthlens.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/public/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("TruthLens Backend is running and connected to DB (tested implicitly on startup)");
    }

    @GetMapping("/users/me")
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");

        // Find or create user based on Keycloak JWT
        Optional<User> optionalUser = userRepository.findByKeycloakId(keycloakId);
        
        User user = optionalUser.orElseGet(() -> {
            User newUser = new User(keycloakId, email, firstName, lastName);
            return userRepository.save(newUser);
        });

        return ResponseEntity.ok(user);
    }
}
