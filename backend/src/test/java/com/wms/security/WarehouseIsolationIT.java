package com.wms.security;

import com.wms.entity.User;
import com.wms.entity.UserWarehouseAssignment;
import com.wms.entity.Warehouse;
import com.wms.enums.UserRole;
import com.wms.repository.UserRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.util.JwtUtil;
import com.wms.enums.WarehouseType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:isolationtestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
public class WarehouseIsolationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private UserWarehouseAssignmentRepository assignmentRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private User storekeeper1;
    private Warehouse warehouse1;
    private Warehouse warehouse2;

    @BeforeEach
    void setUp() {
        assignmentRepository.deleteAll();
        userRepository.deleteAll();
        warehouseRepository.deleteAll();

        // 1. Create two warehouses
        warehouse1 = Warehouse.builder()
                .code("WH-HP")
                .name("Hai Phong Warehouse")
                .type(WarehouseType.PHYSICAL)
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        warehouse1 = warehouseRepository.save(warehouse1);

        warehouse2 = Warehouse.builder()
                .code("WH-HN")
                .name("Ha Noi Warehouse")
                .type(WarehouseType.PHYSICAL)
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        warehouse2 = warehouseRepository.save(warehouse2);

        // 2. Create storekeeper user
        storekeeper1 = User.builder()
                .code("SK001")
                .fullName("Storekeeper 1")
                .email("sk1@wms.com")
                .passwordHash("$2a$12$DummyHashedPasswordForTestingPurposesOnly")
                .role(UserRole.STOREKEEPER)
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        storekeeper1 = userRepository.save(storekeeper1);

        // 3. Assign storekeeper1 only to Warehouse 1
        UserWarehouseAssignment assignment = new UserWarehouseAssignment();
        assignment.setUser(storekeeper1);
        assignment.setWarehouse(warehouse1);
        assignment.setAssignedBy(storekeeper1);
        assignment.setAssignedAt(OffsetDateTime.now());
        assignmentRepository.save(assignment);
    }

    @Test
    void warehouseIsolation_authorizedWarehouse_returns200() throws Exception {
        String token = jwtUtil.generateAccessToken("sk1@wms.com", "STOREKEEPER");

        // Should allow access to Warehouse 1
        mockMvc.perform(get("/api/v1/receipts")
                        .param("warehouseId", warehouse1.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void warehouseIsolation_unauthorizedWarehouse_returns403() throws Exception {
        String token = jwtUtil.generateAccessToken("sk1@wms.com", "STOREKEEPER");

        // Should forbid access to Warehouse 2
        mockMvc.perform(get("/api/v1/receipts")
                        .param("warehouseId", warehouse2.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
