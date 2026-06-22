package org.example.discoveryserver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DiscoveryServerApplicationTests {

    @Test
    void contextLoads() {
        Assertions.assertDoesNotThrow(() -> {});
    }

    @Test
    void mainMethodLoads() {
        Assertions.assertDoesNotThrow(() -> DiscoveryServerApplication.main(new String[] {"--server.port=0"}));
    }
}
