package com.truthlens.ocr.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Test error");
        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgumentException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("Test error");
    }

    @Test
    void handleIllegalStateException() {
        IllegalStateException ex = new IllegalStateException("Test state error");
        ResponseEntity<Map<String, String>> response = handler.handleIllegalStateException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("Test state error");
    }
}
