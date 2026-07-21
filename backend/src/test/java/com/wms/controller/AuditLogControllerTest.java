package com.wms.controller;

import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.dto.response.AuditLogDetailResponse;
import com.wms.dto.response.AuditLogListItemResponse;
import com.wms.dto.response.AuditLogPageResponse;
import com.wms.entity.User;
import com.wms.enums.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.repository.UserRepository;
import com.wms.service.AuditLogService;
import com.wms.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditLogController.class)
@Import({ SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class })
class AuditLogControllerTest {

        @Autowired
        MockMvc mockMvc;

        @MockBean
        AuditLogService auditLogService;
        @MockBean
        UserRepository userRepository;
        @MockBean
        JwtUtil jwtUtil;
        @MockBean
        UserDetailsServiceImpl userDetailsService;

        private User adminUser;
        private User ceoUser;
        private User storekeepUser;

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

                storekeepUser = new User();
                storekeepUser.setId(3L);
                storekeepUser.setEmail("store@wms.com");
                storekeepUser.setRole(UserRole.STOREKEEPER);
        }

        // ─── GET /api/v1/admin/audit-logs ─────────────────────────────────────────

        @Test
        @DisplayName("GET /admin/audit-logs — 200 OK khi ADMIN truy cập")
        @WithMockUser(username = "admin@wms.com", roles = "ADMIN")
        void getAuditLogs_admin_returns200() throws Exception {
                when(userRepository.findByEmail("admin@wms.com")).thenReturn(Optional.of(adminUser));

                AuditLogListItemResponse item = buildListItem(10L, "UPDATE", "User");
                when(auditLogService.getAuditLogs(any(), any(), any(), any(), any()))
                                .thenReturn(new AuditLogPageResponse(List.of(item), 1, 30, false, false, false));

                mockMvc.perform(get("/api/v1/admin/audit-logs").contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.page").value(1))
                                .andExpect(jsonPath("$.pageSize").value(30))
                                .andExpect(jsonPath("$.data[0].id").value(10))
                                .andExpect(jsonPath("$.data[0].action").value("UPDATE"))
                                .andExpect(jsonPath("$.data[0].entityType").value("User"));
        }

        @Test
        @DisplayName("GET /admin/audit-logs — 403 FORBIDDEN_AUDIT_ACCESS khi CEO truy cập")
        @WithMockUser(username = "ceo@wms.com", roles = "CEO")
        void getAuditLogs_ceo_returns403() throws Exception {
                mockMvc.perform(get("/api/v1/admin/audit-logs").contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /admin/audit-logs — 403 khi STOREKEEPER truy cập")
        @WithMockUser(username = "store@wms.com", roles = "STOREKEEPER")
        void getAuditLogs_storekeeper_returns403() throws Exception {
                mockMvc.perform(get("/api/v1/admin/audit-logs").contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /admin/audit-logs — 401 khi không có token")
        void getAuditLogs_unauthenticated_returns403() throws Exception {
                mockMvc.perform(get("/api/v1/admin/audit-logs").contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /admin/audit-logs — 400 QUERY_RANGE_TOO_LARGE khi page > 50 không có filter")
        @WithMockUser(username = "admin@wms.com", roles = "ADMIN")
        void getAuditLogs_page51NoFilter_returns400() throws Exception {
                when(userRepository.findByEmail("admin@wms.com")).thenReturn(Optional.of(adminUser));
                when(auditLogService.getAuditLogs(eq(51), any(), isNull(), isNull(), isNull()))
                                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "QUERY_RANGE_TOO_LARGE"));

                mockMvc.perform(get("/api/v1/admin/audit-logs").param("page", "51").contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /admin/audit-logs — 400 INVALID_DATE_RANGE khi from > to")
        @WithMockUser(username = "admin@wms.com", roles = "ADMIN")
        void getAuditLogs_fromAfterTo_returns400() throws Exception {
                when(userRepository.findByEmail("admin@wms.com")).thenReturn(Optional.of(adminUser));
                when(auditLogService.getAuditLogs(any(), any(), any(), any(), any()))
                                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE"));

                mockMvc.perform(get("/api/v1/admin/audit-logs")
                                .param("from", "2026-12-31")
                                .param("to", "2026-01-01")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /admin/audit-logs — 200 với filter from/to và warehouseId")
        @WithMockUser(username = "admin@wms.com", roles = "ADMIN")
        void getAuditLogs_withFilters_returns200() throws Exception {
                when(userRepository.findByEmail("admin@wms.com")).thenReturn(Optional.of(adminUser));

                AuditLogListItemResponse item = buildListItem(5L, "CREATE", "Product");
                when(auditLogService.getAuditLogs(any(), any(), eq("2026-01-01"), eq("2026-12-31"), eq(1L)))
                                .thenReturn(new AuditLogPageResponse(List.of(item), 1, 30, false, false, false));

                mockMvc.perform(get("/api/v1/admin/audit-logs")
                                .param("from", "2026-01-01")
                                .param("to", "2026-12-31")
                                .param("warehouse_id", "1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].entityType").value("Product"));
        }

        @Test
        @DisplayName("GET /admin/audit-logs â€” váº«n há»— trá»£ alias warehouseId")
        @WithMockUser(username = "admin@wms.com", roles = "ADMIN")
        void getAuditLogs_withWarehouseIdAlias_returns200() throws Exception {
                when(userRepository.findByEmail("admin@wms.com")).thenReturn(Optional.of(adminUser));
                when(auditLogService.getAuditLogs(any(), any(), isNull(), isNull(), eq(2L)))
                                .thenReturn(new AuditLogPageResponse(List.of(), 1, 30, false, false, false));

                mockMvc.perform(get("/api/v1/admin/audit-logs")
                                .param("warehouseId", "2")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /admin/audit-logs — hasNext đúng khi còn trang tiếp theo")
        @WithMockUser(username = "admin@wms.com", roles = "ADMIN")
        void getAuditLogs_hasNextTrue_returnedCorrectly() throws Exception {
                when(userRepository.findByEmail("admin@wms.com")).thenReturn(Optional.of(adminUser));
                when(auditLogService.getAuditLogs(any(), any(), any(), any(), any()))
                                .thenReturn(new AuditLogPageResponse(
                                                List.of(buildListItem(1L, "LOGIN", "User")), 1, 30, true, false,
                                                false));

                mockMvc.perform(get("/api/v1/admin/audit-logs").contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.hasNext").value(true))
                                .andExpect(jsonPath("$.hasPrevious").value(false));
        }

        @Test
        @DisplayName("GET /admin/audit-logs — Kết quả có đầy đủ các field của list item")
        @WithMockUser(username = "admin@wms.com", roles = "ADMIN")
        void getAuditLogs_listItemHasAllFields() throws Exception {
                when(userRepository.findByEmail("admin@wms.com")).thenReturn(Optional.of(adminUser));

                AuditLogListItemResponse item = buildListItem(7L, "STATUS_CHANGE", "Receipt");
                item.setActorName("Nguyen Van A");
                item.setActorRole("STOREKEEPER");
                item.setDescription("STATUS_CHANGE Receipt R001");
                item.setEntityId(99L);

                when(auditLogService.getAuditLogs(any(), any(), any(), any(), any()))
                                .thenReturn(new AuditLogPageResponse(List.of(item), 1, 30, false, false, false));

                mockMvc.perform(get("/api/v1/admin/audit-logs").contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].actorName").value("Nguyen Van A"))
                                .andExpect(jsonPath("$.data[0].actorRole").value("STOREKEEPER"))
                                .andExpect(jsonPath("$.data[0].description").value("STATUS_CHANGE Receipt R001"))
                                .andExpect(jsonPath("$.data[0].entityId").value(99));
        }

        // ─── GET /api/v1/admin/audit-logs/{id} ───────────────────────────────────

        @Test
        @DisplayName("GET /admin/audit-logs/{id} — 200 OK với chi tiết đầy đủ khi ADMIN truy cập")
        @WithMockUser(username = "admin@wms.com", roles = "ADMIN")
        void getAuditLogById_admin_returns200WithDetail() throws Exception {
                when(userRepository.findByEmail("admin@wms.com")).thenReturn(Optional.of(adminUser));

                AuditLogDetailResponse detail = buildDetailResponse(10L);
                when(auditLogService.getAuditLogById(10L)).thenReturn(detail);

                mockMvc.perform(get("/api/v1/admin/audit-logs/10").contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(10))
                                .andExpect(jsonPath("$.actorName").value("System Admin"))
                                .andExpect(jsonPath("$.action").value("UPDATE"))
                                .andExpect(jsonPath("$.entityType").value("User"))
                                .andExpect(jsonPath("$.oldValue.status").value("OLD"))
                                .andExpect(jsonPath("$.newValue.status").value("NEW"));
        }

        @Test
        @DisplayName("GET /admin/audit-logs/{id} — 404 AUDIT_LOG_NOT_FOUND khi ID không tồn tại")
        @WithMockUser(username = "admin@wms.com", roles = "ADMIN")
        void getAuditLogById_notFound_returns404() throws Exception {
                when(userRepository.findByEmail("admin@wms.com")).thenReturn(Optional.of(adminUser));
                when(auditLogService.getAuditLogById(999L))
                                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "AUDIT_LOG_NOT_FOUND"));

                mockMvc.perform(get("/api/v1/admin/audit-logs/999").contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /admin/audit-logs/{id} — 403 khi CEO truy cập")
        @WithMockUser(username = "ceo@wms.com", roles = "CEO")
        void getAuditLogById_ceo_returns403() throws Exception {
                mockMvc.perform(get("/api/v1/admin/audit-logs/1").contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /admin/audit-logs/{id} — 401 khi không có token")
        void getAuditLogById_unauthenticated_returns403() throws Exception {
                mockMvc.perform(get("/api/v1/admin/audit-logs/1").contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized());
        }

        // ─── Helper ──────────────────────────────────────────────────────────────

        private AuditLogListItemResponse buildListItem(Long id, String action, String entityType) {
                AuditLogListItemResponse item = new AuditLogListItemResponse();
                item.setId(id);
                item.setAction(action);
                item.setEntityType(entityType);
                item.setEntityId(1L);
                item.setActorName("System Admin");
                item.setActorRole("ADMIN");
                item.setDescription(action + " " + entityType + " 1");
                item.setTimestamp(OffsetDateTime.now());
                return item;
        }

        private AuditLogDetailResponse buildDetailResponse(Long id) {
                AuditLogDetailResponse detail = new AuditLogDetailResponse();
                detail.setId(id);
                detail.setActorName("System Admin");
                detail.setActorRole("ADMIN");
                detail.setAction("UPDATE");
                detail.setEntityType("User");
                detail.setEntityId(1L);
                detail.setTimestamp(OffsetDateTime.now());
                detail.setOldValue(java.util.Map.of("status", "OLD"));
                detail.setNewValue(java.util.Map.of("status", "NEW"));
                return detail;
        }
}


