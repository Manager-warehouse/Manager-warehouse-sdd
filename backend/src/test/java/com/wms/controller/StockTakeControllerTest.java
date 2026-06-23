package com.wms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.dto.response.StockTakeResponse;
import com.wms.dto.response.StockTakeSummaryResponse;
import com.wms.entity.AccountingPeriod;
import com.wms.entity.StockTake;
import com.wms.entity.User;
import com.wms.entity.Warehouse;
import com.wms.enums.ApprovalLevel;
import com.wms.enums.StockTakeStatus;
import com.wms.enums.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.exception.StockTakeException;
import com.wms.service.CurrentUserService;
import com.wms.service.StockTakeService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.mockito.Mock;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * API-layer tests for StockTakeController — Spec 006 (US-WMS-13).
 *
 * <p>
 * Covers happy paths, validation (400), unauthenticated (401), role-scope
 * (403),
 * and business-error mapping (409) through GlobalExceptionHandler.
 * </p>
 */
@WebMvcTest(StockTakeController.class)
@Import({ SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class })
class StockTakeControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Mock
        private StockTakeService stockTakeService;
        @Mock
        private CurrentUserService currentUserService;
        @Mock
        private com.wms.repository.UserRepository userRepository;
        @Mock
        private com.wms.util.JwtUtil jwtUtil;
        @Mock
        private com.wms.config.UserDetailsServiceImpl userDetailsService;

        private User manager;

        @BeforeEach
        void setUp() {
                manager = new User();
                manager.setId(5L);
                manager.setRole(UserRole.WAREHOUSE_MANAGER);
                when(currentUserService.getRequiredCurrentUser()).thenReturn(manager);
        }

        private StockTakeResponse sampleResponse(StockTakeStatus status, ApprovalLevel level) {
                Warehouse wh = new Warehouse();
                wh.setId(10L);
                wh.setName("Kho Hải Phòng");

                User sk = new User();
                sk.setId(3L);
                sk.setFullName("Nguyễn Thủ Kho");

                AccountingPeriod period = new AccountingPeriod();
                period.setId(1L);
                period.setPeriodName("T06/2026");

                StockTake st = StockTake.builder()
                                .id(1L)
                                .stockTakeNumber("ST-20260617-000001")
                                .warehouse(wh)
                                .conductedBy(sk)
                                .status(status)
                                .approvalLevel(level)
                                .isEmployeeFault(false)
                                .totalVarianceValue(new BigDecimal("-1000000"))
                                .stockTakeDate(LocalDate.of(2026, 6, 17))
                                .documentDate(LocalDate.of(2026, 6, 17))
                                .accountingPeriod(period)
                                .build();
                return StockTakeResponse.from(st, List.of());
        }

        // ─── List & Detail ──────────────────────────────────────────────────────────

        @Test
        @WithMockUser(username = "sk@wms.com", roles = "STOREKEEPER")
        void getStockTakes_authenticated_returns200() throws Exception {
                StockTakeSummaryResponse summary = StockTakeSummaryResponse.from(StockTake.builder()
                                .id(1L).stockTakeNumber("ST-20260617-000001")
                                .warehouse(warehouse10()).conductedBy(skUser())
                                .status(StockTakeStatus.DRAFT)
                                .stockTakeDate(LocalDate.of(2026, 6, 17))
                                .build());
                when(stockTakeService.getStockTakes(eq(10L), any(), any())).thenReturn(List.of(summary));

                mockMvc.perform(get("/api/v1/stocktakes").param("warehouse_id", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].stock_take_number").value("ST-20260617-000001"))
                                .andExpect(jsonPath("$[0].status").value("DRAFT"));
        }

        @Test
        void getStockTakes_unauthenticated_isDenied() throws Exception {
                // No JWT → security entry point returns 401 Unauthorized.
                mockMvc.perform(get("/api/v1/stocktakes").param("warehouse_id", "10"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "sk@wms.com", roles = "STOREKEEPER")
        void getStockTakeById_returns200WithDetail() throws Exception {
                when(stockTakeService.getStockTakeById(eq(1L), any()))
                                .thenReturn(sampleResponse(StockTakeStatus.PENDING_APPROVAL, ApprovalLevel.MANAGER));

                mockMvc.perform(get("/api/v1/stocktakes/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.approval_level").value("MANAGER"))
                                .andExpect(jsonPath("$.warehouse_name").value("Kho Hải Phòng"));
        }

        // ─── Create ─────────────────────────────────────────────────────────────────

        @Test
        @WithMockUser(username = "sk@wms.com", roles = "STOREKEEPER")
        void createStockTake_validBody_returns201() throws Exception {
                when(stockTakeService.createStockTake(any(), any()))
                                .thenReturn(sampleResponse(StockTakeStatus.DRAFT, null));

                String body = "{\"warehouse_id\":10,\"stock_take_date\":\"2026-06-17\","
                                + "\"document_date\":\"2026-06-17\",\"accounting_period_id\":1}";

                mockMvc.perform(post("/api/v1/stocktakes").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.status").value("DRAFT"));
        }

        @Test
        @WithMockUser(username = "sk@wms.com", roles = "STOREKEEPER")
        void createStockTake_missingRequiredFields_returns400() throws Exception {
                String body = "{\"notes\":\"missing required fields\"}";

                mockMvc.perform(post("/api/v1/stocktakes").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void createStockTake_unauthenticated_isDenied() throws Exception {
                String body = "{\"warehouse_id\":10,\"stock_take_date\":\"2026-06-17\","
                                + "\"document_date\":\"2026-06-17\",\"accounting_period_id\":1}";

                // No JWT → security entry point returns 401 Unauthorized.
                mockMvc.perform(post("/api/v1/stocktakes").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                                .andExpect(status().isUnauthorized());
        }

        // ─── Lifecycle transitions ──────────────────────────────────────────────────

        @Test
        @WithMockUser(username = "sk@wms.com", roles = "STOREKEEPER")
        void startStockTake_returns200() throws Exception {
                when(stockTakeService.startStockTake(eq(1L), any()))
                                .thenReturn(sampleResponse(StockTakeStatus.IN_PROGRESS, null));

                mockMvc.perform(put("/api/v1/stocktakes/1/start").with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }

        @Test
        @WithMockUser(username = "sk@wms.com", roles = "STOREKEEPER")
        void recordCount_returns200() throws Exception {
                when(stockTakeService.recordCount(eq(1L), any(), any()))
                                .thenReturn(sampleResponse(StockTakeStatus.IN_PROGRESS, null));

                String body = "{\"items\":[{\"item_id\":50,\"actual_qty\":88}]}";

                mockMvc.perform(put("/api/v1/stocktakes/1/count").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "sk@wms.com", roles = "STOREKEEPER")
        void completeStockTake_returns200() throws Exception {
                when(stockTakeService.completeStockTake(eq(1L), any()))
                                .thenReturn(sampleResponse(StockTakeStatus.PENDING_APPROVAL, ApprovalLevel.MANAGER));

                mockMvc.perform(put("/api/v1/stocktakes/1/complete").with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
        }

        // ─── Approve / Reject (role-gated) ──────────────────────────────────────────

        @Test
        @WithMockUser(username = "mgr@wms.com", roles = "WAREHOUSE_MANAGER")
        void approveStockTake_managerRole_returns200() throws Exception {
                when(stockTakeService.approveStockTake(eq(1L), any()))
                                .thenReturn(sampleResponse(StockTakeStatus.APPROVED, ApprovalLevel.MANAGER));

                mockMvc.perform(put("/api/v1/stocktakes/1/approve").with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("APPROVED"));
        }

        @Test
        @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
        void approveStockTake_forbiddenRole_returns403() throws Exception {
                mockMvc.perform(put("/api/v1/stocktakes/1/approve").with(csrf()))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "mgr@wms.com", roles = "WAREHOUSE_MANAGER")
        void approveStockTake_alreadyApproved_returns409() throws Exception {
                when(stockTakeService.approveStockTake(eq(1L), any()))
                                .thenThrow(new StockTakeException("STOCK_TAKE_ALREADY_APPROVED",
                                                HttpStatus.CONFLICT, "StockTake is already APPROVED"));

                mockMvc.perform(put("/api/v1/stocktakes/1/approve").with(csrf()))
                                .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(username = "mgr@wms.com", roles = "WAREHOUSE_MANAGER")
        void rejectStockTake_withReason_returns200() throws Exception {
                when(stockTakeService.rejectStockTake(eq(1L), any(), any()))
                                .thenReturn(sampleResponse(StockTakeStatus.REJECTED, ApprovalLevel.MANAGER));

                String body = "{\"rejection_reason\":\"Recount needed\"}";

                mockMvc.perform(put("/api/v1/stocktakes/1/reject").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("REJECTED"));
        }

        @Test
        @WithMockUser(username = "mgr@wms.com", roles = "WAREHOUSE_MANAGER")
        void rejectStockTake_missingReason_returns400() throws Exception {
                String body = "{\"rejection_reason\":\"\"}";

                mockMvc.perform(put("/api/v1/stocktakes/1/reject").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "mgr@wms.com", roles = "WAREHOUSE_MANAGER")
        void approveCeoStockTake_managerRole_returns403() throws Exception {
                mockMvc.perform(put("/api/v1/stocktakes/1/approve-ceo").with(csrf()))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "ceo@wms.com", roles = "CEO")
        void approveCeoStockTake_ceoRole_returns200() throws Exception {
                when(stockTakeService.approveCeoStockTake(eq(1L), any()))
                                .thenReturn(sampleResponse(StockTakeStatus.APPROVED, ApprovalLevel.CEO));

                mockMvc.perform(put("/api/v1/stocktakes/1/approve-ceo").with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.approval_level").value("CEO"));
        }

        @Test
        @WithMockUser(username = "sk@wms.com", roles = "STOREKEEPER")
        void cancelStockTake_returns200() throws Exception {
                when(stockTakeService.cancelStockTake(eq(1L), any()))
                                .thenReturn(sampleResponse(StockTakeStatus.CANCELLED, null));

                mockMvc.perform(put("/api/v1/stocktakes/1/cancel").with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        // ─── Helpers ────────────────────────────────────────────────────────────────

        private Warehouse warehouse10() {
                Warehouse wh = new Warehouse();
                wh.setId(10L);
                wh.setName("Kho Hải Phòng");
                return wh;
        }

        private User skUser() {
                User sk = new User();
                sk.setId(3L);
                sk.setFullName("Nguyễn Thủ Kho");
                return sk;
        }
}
