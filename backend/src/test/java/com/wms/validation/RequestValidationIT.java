package com.wms.validation;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:validationtestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "spring.mail.host=localhost",
    "spring.mail.port=25",
    "jwt.secret=9a4f2c8d3b7a1e5f8c2d6e0b4a8f9c1d3e7b2a6f0c4d8e2f6a0b4c8d2e6f0a4b",
    "jwt.access-token-expiry=900",
    "jwt.refresh-token-expiry=604800"
})
public class RequestValidationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private String adminToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User admin = User.builder()
                .code("ADM001")
                .fullName("Admin User")
                .email("admin@wms.com")
                .passwordHash("$2a$12$DummyHashedPasswordForTestingPurposesOnly")
                .role(UserRole.ADMIN)
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        userRepository.save(admin);

        adminToken = jwtUtil.generateAccessToken("admin@wms.com", "ADMIN");
    }

    @Test
    void createWarehouse_missingFields_returns400ValidationError() throws Exception {
        // Send request with missing code, name, and type
        String requestJson = "{}";

        mockMvc.perform(post("/api/v1/admin/warehouses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.code").value("CODE_REQUIRED"))
                .andExpect(jsonPath("$.details.name").value("NAME_REQUIRED"))
                .andExpect(jsonPath("$.details.type").value("TYPE_REQUIRED"));
    }

    @Test
    void createWarehouse_invalidWarehouseType_returns400ValidationError() throws Exception {
        // Send request with invalid type
        String requestJson = "{"
                + "\"code\": \"WH-HP\","
                + "\"name\": \"Hai Phong Warehouse\","
                + "\"type\": \"INVALID_TYPE\""
                + "}";

        mockMvc.perform(post("/api/v1/admin/warehouses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.type").value("INVALID_WAREHOUSE_TYPE"));
    }
}
