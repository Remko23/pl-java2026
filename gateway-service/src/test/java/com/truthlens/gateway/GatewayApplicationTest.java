package com.truthlens.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.cloud.discovery.enabled=false"
})
class GatewayApplicationTest {

    @Test
    void contextLoads() {
        // Verifies the application context loads successfully
    }

    @Test
    void mainMethodLoads() {
        // Just calling main method for coverage without starting the server
        GatewayServiceApplication.main(new String[] {"--server.port=0", "--spring.cloud.discovery.enabled=false"});
    }
}
