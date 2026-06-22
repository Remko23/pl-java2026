package org.example.configserver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ConfigServerApplicationTests {

    @Test
    void contextLoads() {
        Assertions.assertDoesNotThrow(() -> {});
    }

    @Test
    void mainMethodLoads() {
        Assertions.assertDoesNotThrow(() -> ConfigServerApplication.main(new String[] {"--server.port=0"}));
    }
}
