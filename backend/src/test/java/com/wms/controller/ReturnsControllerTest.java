package com.wms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.controller.return_disposal.ReturnsController;
import com.wms.dto.response.CreditNoteResponse;
import com.wms.dto.response.ReceiptActionResponse;
import com.wms.entity.access_control.User;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.stock_receiving.ReceiptStatus;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.service.return_disposal.ReturnsService;
import com.wms.service.user_context.CurrentUserService;
import com.wms.util.JwtUtil;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReturnsController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class ReturnsControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ReturnsService returnsService;
    @MockBean private CurrentUserService currentUserService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private User storekeeper;

    @BeforeEach
    void setUp() {
        storekeeper = new User();
        storekeeper.setId(30L);
        storekeeper.setEmail("storekeeper@wms.com");
        storekeeper.setRole(UserRole.STOREKEEPER);

        when(currentUserService.getRequiredCurrentUser()).thenReturn(storekeeper);
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void createReturnReceipt_success() throws Exception {
        ReceiptActionResponse response = ReceiptActionResponse.builder()
                .id(400L)
                .receiptNumber("REC-RET-001")
                .status(ReceiptStatus.DRAFT)
                .build();

        when(returnsService.createReturnReceipt(any(), eq(storekeeper))).thenReturn(response);

        String json = "{\"warehouseId\":1,\"deliveryOrderId\":100,\"dealerId\":5,\"notes\":\"Wrong item size\",\"items\":[{\"productId\":100,\"expectedQty\":10}]}";

        mockMvc.perform(post("/api/v1/returns")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiptNumber").value("REC-RET-001"));
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void processReturnQc_success() throws Exception {
        ReceiptActionResponse response = ReceiptActionResponse.builder()
                .id(400L)
                .status(ReceiptStatus.APPROVED)
                .build();

        when(returnsService.processReturnQc(eq(400L), any(), eq(storekeeper))).thenReturn(response);

        String json = "{\"expectedVersion\":0,\"items\":[{\"receiptItemId\":1,\"actualQty\":10,\"passedQty\":8,\"failedQty\":2,\"passedLocationId\":101,\"quarantineLocationId\":102}]}";

        mockMvc.perform(put("/api/v1/returns/400/qc")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void createCreditNote_success() throws Exception {
        CreditNoteResponse response = CreditNoteResponse.builder()
                .creditNoteId(90L)
                .creditNoteNumber("CN-2026-001")
                .amount(new BigDecimal("2000000"))
                .build();

        when(returnsService.createCreditNote(eq(400L), any(), any())).thenReturn(response);

        String json = "{\"reason\":\"Credit note for returned goods\"}";

        mockMvc.perform(post("/api/v1/returns/400/credit-note")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditNoteNumber").value("CN-2026-001"));
    }
}
