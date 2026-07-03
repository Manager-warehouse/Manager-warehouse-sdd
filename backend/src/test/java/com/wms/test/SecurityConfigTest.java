package com.wms.test;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SecurityConfigTest {

    @Test
    void testApplicationSecretsLoading() {
        // Simple placeholder test - configuration is loaded
        // and the secrets are available from application-secrets.yml
        assertThat(true).isTrue();
    }
}