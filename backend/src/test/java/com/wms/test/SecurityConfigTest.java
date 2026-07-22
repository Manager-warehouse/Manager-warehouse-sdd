package com.wms.test;


import com.wms.entity.access_control.*;
import com.wms.entity.audit_trail.*;
import com.wms.entity.billing_payment.*;
import com.wms.entity.dealer_management.*;
import com.wms.entity.document_numbering.*;
import com.wms.entity.driver_management.*;
import com.wms.entity.fleet_management.*;
import com.wms.entity.notification_delivery.*;
import com.wms.entity.order_fulfillment.*;
import com.wms.entity.price_management.*;
import com.wms.entity.product_catalog.*;
import com.wms.entity.stock_control.*;
import com.wms.entity.stock_counting.*;
import com.wms.entity.stock_receiving.*;
import com.wms.entity.supplier_management.*;
import com.wms.entity.user_configuration.*;
import com.wms.entity.warehouse_location.*;
import com.wms.entity.warehouse_transfer.*;
import com.wms.enums.access_control.*;
import com.wms.enums.audit_trail.*;
import com.wms.enums.billing_payment.*;
import com.wms.enums.dealer_management.*;
import com.wms.enums.driver_management.*;
import com.wms.enums.fleet_management.*;
import com.wms.enums.notification_delivery.*;
import com.wms.enums.order_fulfillment.*;
import com.wms.enums.price_management.*;
import com.wms.enums.stock_control.*;
import com.wms.enums.stock_counting.*;
import com.wms.enums.stock_receiving.*;
import com.wms.enums.supplier_management.*;
import com.wms.enums.user_configuration.*;
import com.wms.enums.warehouse_location.*;
import com.wms.enums.warehouse_transfer.*;
import com.wms.entity.access_control.User;
import com.wms.enums.access_control.UserRole;
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

    @Test
    void admin_canAccessAccountantEndpoints_viaRoleHierarchy() throws Exception {
        String token = jwtUtil.generateAccessToken("test@wms.com", "ADMIN");

        // POST /api/v1/suppliers requires ROLE_ACCOUNTANT or ROLE_ACCOUNTANT_MANAGER.
        // With Role Hierarchy, ADMIN should bypass 403 and hit validation or other errors (e.g. 400 Bad Request) instead of 403.
        mockMvc.perform(post("/api/v1/suppliers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest()); // validation fails, but not 403 Forbidden
    }
}