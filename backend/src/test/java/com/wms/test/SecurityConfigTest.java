package com.wms.test;

import com.wms.entity.User;
import com.wms.enums.UserRole;
import com.wms.repository.UserRepository;
import com.wms.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:securitytestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.username=sa",
    "spring.datasource.password=", // test
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "spring.mail.host=localhost",
    "spring.mail.port=25",
    "jwt.secret=9a4f2c8d3b7a1e5f8c2d6e0b4a8f9c1d3e7b2a6f0c4d8e2f6a0b4c8d2e6f0a4b", // test
    "jwt.access-token-expiry=900",
    "jwt.refresh-token-expiry=604800"
})
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User testUser = User.builder()
                .code("EMP001")
                .fullName("Test User")
                .email("test@wms.com")
                .passwordHash("$2a$12$DummyHashedPasswordForTestingPurposesOnly")
                .role(UserRole.ADMIN)
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        userRepository.save(testUser);
    }

    @Test
    void testApplicationSecretsLoading() {
        assertThat(jwtUtil).isNotNull();
    }

    @Test
    void publicEndpoint_isPermitted() throws Exception {
        // Accessing login endpoint without token should not return 401 Unauthorized from filter chain.
        // It might return 400 Bad Request because we send empty body, but it is not 401.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpoint_withoutToken_isUnauthorized() throws Exception {
        // Accessing protected endpoint without token should return 401 Unauthorized.
        mockMvc.perform(get("/api/v1/admin/warehouses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withValidToken_isAllowed() throws Exception {
        String token = jwtUtil.generateAccessToken("test@wms.com", "ADMIN");

        // Accessing protected endpoint with valid token should proceed to the controller (returns 200 or other than 401).
        mockMvc.perform(get("/api/v1/admin/warehouses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_withInvalidToken_isUnauthorized() throws Exception {
        String invalidToken = "invalid.jwt.token.string";

        // Accessing protected endpoint with invalid token should return 401 Unauthorized.
        mockMvc.perform(get("/api/v1/admin/warehouses")
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
    }
}