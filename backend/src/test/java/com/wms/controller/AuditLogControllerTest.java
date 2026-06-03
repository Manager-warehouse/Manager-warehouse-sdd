package com.wms.controller;

import com.wms.dto.AuditLogDetailResponse;
import com.wms.dto.AuditLogListItemResponse;
import com.wms.dto.AuditLogPageResponse;
import com.wms.entity.User;
import com.wms.enums.UserRole;
import com.wms.repository.UserRepository;
import com.wms.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
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

    private User adminUser;
    private User ceoUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setEmail("admin@wms.com");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setFullName("System Admin");

        ceoUser = new User();
        ceoUser.setId(2L);
        ceoUser.setEmail("ceo@wms.com");
        ceoUser.setRole(UserRole.CEO);
        ceoUser.setFullName("CEO");
    }

    @Test
    @WithMockUser(username = "admin@wms.com", roles = "ADMIN")
    void testGetAuditLogs_adminSuccess() throws Exception {
        when(userRepository.findByEmail("admin@wms.com")).thenReturn(Optional.of(adminUser));

        AuditLogListItemResponse item = new AuditLogListItemResponse();
        item.setId(10L);
        item.setActorName("System Admin");
        item.setAction("UPDATE");
        item.setEntityType("User");
        item.setEntityId(1L);
        item.setDescription("UPDATE User 1");
        item.setTimestamp(OffsetDateTime.now());

        when(auditLogService.getAuditLogs(any(), any(), any(), any(), any()))
                .thenReturn(new AuditLogPageResponse(List.of(item), 1, 30, false, false, false));

        mockMvc.perform(get("/api/v1/audit-logs")
                        .param("page", "1")
                        .param("pageSize", "30")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    @WithMockUser(username = "ceo@wms.com", roles = "CEO")
    void testGetAuditLogs_ceoForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@wms.com", roles = "ADMIN")
    void testGetAuditLogById_adminSuccess() throws Exception {
        when(userRepository.findByEmail("admin@wms.com")).thenReturn(Optional.of(adminUser));

        AuditLogDetailResponse detail = new AuditLogDetailResponse();
        detail.setId(10L);
        detail.setActorName("System Admin");
        detail.setAction("UPDATE");
        when(auditLogService.getAuditLogById(10L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/audit-logs/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }
}
