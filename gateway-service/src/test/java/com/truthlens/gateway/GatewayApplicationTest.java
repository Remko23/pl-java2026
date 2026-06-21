package com.truthlens.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.cloud.discovery.enabled=false"
})
class GatewayApplicationTest {

    @Test
    void contextLoads() {
    }

    @Test
    void mainMethodLoads() {
        GatewayServiceApplication.main(new String[] {"--server.port=0", "--spring.cloud.discovery.enabled=false"});
    }
}
