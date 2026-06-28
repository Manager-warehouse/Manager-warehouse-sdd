package com.wms.test;

import com.wms.dto.auth.LoginRequest;
import com.wms.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(locations = "classpath:application-secrets.yml")
public class AuthServiceLoginTest {

    @Autowired
    private AuthService authService;

    @Test
    void testAuthServiceConfig() {
        assertNotNull(authService);
    }
}