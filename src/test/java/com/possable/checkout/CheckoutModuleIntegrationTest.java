package com.possable.checkout;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.test.ApplicationModuleTest;

/**
 * Integration test for the `checkout` application module.
 * Bootstraps only the module using @ApplicationModuleTest.
 */
@ApplicationModuleTest
class CheckoutModuleIntegrationTest {

    @Test
    void contextLoads() {
        // If the module context boots successfully, wiring is valid.
    }
} 