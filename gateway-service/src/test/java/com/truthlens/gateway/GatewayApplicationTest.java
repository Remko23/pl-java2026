package com.truthlens.gateway;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.cloud.discovery.enabled=false"
})
class GatewayApplicationTest {

    @Test
    void contextLoads() {
        Assertions.assertDoesNotThrow(() -> {});
    }

    @Test
    void mainMethodLoads() {
        Assertions.assertDoesNotThrow(() -> GatewayServiceApplication.main(new String[] { "--server.port=0", "--spring.cloud.discovery.enabled=false" }));
    }
}
