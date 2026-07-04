package com.wms.test;

import com.wms.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class AuthServiceLoginTest {

    @Autowired
    private AuthService authService;

    @Test
    void testAuthServiceConfig() {
        assertNotNull(authService);
    }
}