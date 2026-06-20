package org.example.discoveryserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DiscoveryServerApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void mainMethodLoads() {
        DiscoveryServerApplication.main(new String[] {"--server.port=0"});
    }
}
