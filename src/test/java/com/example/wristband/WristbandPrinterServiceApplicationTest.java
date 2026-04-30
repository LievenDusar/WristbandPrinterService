package com.example.wristband;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "printer.host=localhost",
        "printer.port=19100",
        "management.server.port=0"   // random free port for test
})
class WristbandPrinterServiceApplicationTest {

    @Test
    void contextLoads() {
        // Verifies that all beans wire up correctly at startup
    }
}
