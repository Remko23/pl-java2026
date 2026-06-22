package com.truthlens.ocr;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.cloud.discovery.enabled=false",
    "spring.cloud.config.enabled=false",
    "spring.cloud.config.fail-fast=false",
    "gemini.api.key=dummy"
})
class OcrServiceApplicationTest {

    @Test
    void contextLoads() {
        Assertions.assertDoesNotThrow(() -> {});
    }

    @Test
    void mainMethodLoads() {
        Assertions.assertDoesNotThrow(() -> OcrServiceApplication.main(new String[] {
            "--server.port=0",
            "--spring.cloud.discovery.enabled=false",
            "--spring.cloud.config.enabled=false",
            "--spring.cloud.config.fail-fast=false",
            "--gemini.api.key=dummy"
        }));
    }
}
