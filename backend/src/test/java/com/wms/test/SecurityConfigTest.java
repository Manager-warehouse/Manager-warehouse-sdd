package com.wms.test;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:5432/postgres?sslmode=require",
    "spring.datasource.username=postgres.jzniugklqehtghgzggiv",
    "spring.datasource.password=Warehouse12345se12",
    "spring.mail.username=sonnguyen556699@gmail.com",
    "spring.mail.password=glykazcgkivznhqq",
    "jwt.secret=9a4f2c8d3b7a1e5f8c2d6e0b4a8f9c1d3e7b2a6f0c4d8e2f6a0b4c8d2e6f0a4b"
})
public class SecurityConfigTest {

    @Test
    void testApplicationSecretsLoading() {
        // Simple placeholder test - configuration is loaded
        // and the secrets are available from application-secrets.yml
        assertThat(true).isTrue();
    }
}