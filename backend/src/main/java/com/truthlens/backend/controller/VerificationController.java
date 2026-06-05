package com.truthlens.backend.controller;

import com.truthlens.backend.model.VerificationRequest;
import com.truthlens.backend.model.VerificationResponse;
import com.truthlens.backend.service.VerificationOrchestratorService;
import com.truthlens.backend.service.VerificationStateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/verifications")
public class VerificationController {

    private final VerificationOrchestratorService orchestratorService;
    private final VerificationStateService stateService;

    public VerificationController(VerificationOrchestratorService orchestratorService, VerificationStateService stateService) {
        this.orchestratorService = orchestratorService;
        this.stateService = stateService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VerificationResponse> verifyText(@RequestBody VerificationRequest request,
                                                           @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt != null ? jwt.getSubject() : null;
        VerificationResponse response = orchestratorService.startVerification(request.claimText(), userId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VerificationResponse> verifyImage(@RequestParam("file") MultipartFile file,
                                                            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt != null ? jwt.getSubject() : null;
        VerificationResponse response = orchestratorService.startVerification(file, userId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VerificationResponse> getStatus(@PathVariable("id") String id) {
        VerificationResponse response = stateService.getState(id);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}

