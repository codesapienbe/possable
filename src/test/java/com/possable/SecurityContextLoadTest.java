package com.possable;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SecurityContextLoadTest {

    @Test
    void contextLoads() {
        // If the Spring context loads, SecurityConfig.filterChain has been executed as part of bean creation.
    }
} 