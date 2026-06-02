package com.wms.controller;

import com.wms.dto.AuditLogPageResponse;
import com.wms.dto.AuditLogResponse;
import com.wms.entity.User;
import com.wms.enums.UserRole;
import com.wms.repository.UserRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditLogController.class)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogService auditLogService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;

    private User adminUser;
    private User managerUser;
    private User staffUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setEmail("admin@wms.com");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setFullName("System Admin");

        managerUser = new User();
        managerUser.setId(2L);
        managerUser.setEmail("manager@wms.com");
        managerUser.setRole(UserRole.WAREHOUSE_MANAGER);
        managerUser.setFullName("Warehouse Manager");

        staffUser = new User();
        staffUser.setId(3L);
        staffUser.setEmail("staff@wms.com");
        staffUser.setRole(UserRole.WAREHOUSE_STAFF);
        staffUser.setFullName("Warehouse Staff");
    }

    @Test
    @WithMockUser(username = "admin@wms.com", roles = "ADMIN")
    void testGetAuditLogs_Admin_Success() throws Exception {
        when(userRepository.findByEmail("admin@wms.com")).thenReturn(Optional.of(adminUser));

        AuditLogResponse logResponse = new AuditLogResponse();
        logResponse.setId(10L);
        logResponse.setActorId(1L);
        logResponse.setActorName("System Admin");
        logResponse.setActorRole("ADMIN");
        logResponse.setAction("CREATE");
        logResponse.setEntityType("RECEIPT");
        logResponse.setEntityId(100L);
        logResponse.setDescription("CREATE RECEIPT PN-001");
        logResponse.setTimestamp(OffsetDateTime.now());

        AuditLogPageResponse pageResponse = new AuditLogPageResponse(
                List.of(logResponse), null, false
        );

        when(auditLogService.getAuditLogs(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/audit-logs")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].description").value("CREATE RECEIPT PN-001"))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @WithMockUser(username = "manager@wms.com", roles = "WAREHOUSE_MANAGER")
    void testGetAuditLogs_Manager_FiltersByAssignedWarehouses() throws Exception {
        when(userRepository.findByEmail("manager@wms.com")).thenReturn(Optional.of(managerUser));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(2L)).thenReturn(List.of(1L, 3L));

        AuditLogPageResponse emptyResponse = new AuditLogPageResponse(List.of(), null, false);
        when(auditLogService.getAuditLogs(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), eq(List.of(1L, 3L))
        )).thenReturn(emptyResponse);

        mockMvc.perform(get("/api/v1/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "manager@wms.com", roles = "WAREHOUSE_MANAGER")
    void testGetAuditLogs_ManagerNoAssignments_ReturnsEmpty() throws Exception {
        when(userRepository.findByEmail("manager@wms.com")).thenReturn(Optional.of(managerUser));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(2L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @WithMockUser(username = "staff@wms.com", roles = "WAREHOUSE_STAFF")
    void testGetAuditLogs_Staff_Forbidden() throws Exception {
        when(userRepository.findByEmail("staff@wms.com")).thenReturn(Optional.of(staffUser));

        mockMvc.perform(get("/api/v1/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
