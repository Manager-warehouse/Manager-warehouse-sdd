package com.wms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wms.dto.response.ReceiptItemResponse;
import com.wms.dto.response.ReceiptResponse;
import com.wms.entity.User;
import com.wms.enums.UserRole;
import com.wms.exception.DuplicateResourceException;
import com.wms.service.CurrentUserService;
import com.wms.service.ReceiptService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReceiptController.class)
class ReceiptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReceiptService receiptService;

    @MockBean
    private CurrentUserService currentUserService;

    private User planner;

    @BeforeEach
    void setUp() {
        planner = new User();
        planner.setId(1L);
        planner.setRole(UserRole.PLANNER);
    }

    @Test
    @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
    void createReceipt_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(planner);
        when(receiptService.createPurchaseReceipt(any(), any())).thenReturn(response());

        mockMvc.perform(post("/api/v1/receipts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receipt_number").value("RN-20260613-0001"))
                .andExpect(jsonPath("$.type").value("PURCHASE"))
                .andExpect(jsonPath("$.status").value("PENDING_RECEIPT"))
                .andExpect(jsonPath("$.items[0].expected_qty").value(500));
    }

    @Test
    @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
    void createReceipt_rejectsMissingMandatoryFields() throws Exception {
        mockMvc.perform(post("/api/v1/receipts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contact_person\":\"A\",\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
    void createReceipt_rejectsInvalidChannel() throws Exception {
        mockMvc.perform(post("/api/v1/receipts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson().replace("ZALO", "PHONE")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
    void createReceipt_rejectsFractionalExpectedQty() throws Exception {
        mockMvc.perform(post("/api/v1/receipts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson().replace("500", "1.5")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
    void createReceipt_rejectsDuplicateSourceReference() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(planner);
        when(receiptService.createPurchaseReceipt(any(), any()))
                .thenThrow(new DuplicateResourceException("duplicate"));

        mockMvc.perform(post("/api/v1/receipts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
    void createReceipt_rejectsReturnFlowField() throws Exception {
        String json = validJson().replace("\"supplier_id\"", "\"type\":\"RETURN\",\"supplier_id\"");

        mockMvc.perform(post("/api/v1/receipts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    private ReceiptResponse response() {
        ReceiptItemResponse item = new ReceiptItemResponse();
        item.setProductId(30L);
        item.setExpectedQty(500);

        ReceiptResponse response = new ReceiptResponse();
        response.setId(100L);
        response.setReceiptNumber("RN-20260613-0001");
        response.setType("PURCHASE");
        response.setStatus("PENDING_RECEIPT");
        response.setSupplierId(10L);
        response.setWarehouseId(20L);
        response.setSourceReference("PO-1");
        response.setSourceChannel("ZALO");
        response.setDocumentDate(LocalDate.of(2026, 6, 13));
        response.setItems(List.of(item));
        return response;
    }

    private String validJson() {
        return """
                {
                  "supplier_id": 10,
                  "contact_person": "Nguyen Van A",
                  "warehouse_id": 20,
                  "source_reference": "PO-1",
                  "source_channel": "ZALO",
                  "items": [
                    {
                      "product_id": 30,
                      "expected_qty": 500
                    }
                  ]
                }
                """;
    }
}
