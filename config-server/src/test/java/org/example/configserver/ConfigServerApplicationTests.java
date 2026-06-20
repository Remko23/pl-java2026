package org.example.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ConfigServerApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void mainMethodLoads() {
        ConfigServerApplication.main(new String[] {"--server.port=0"});
    }
}
