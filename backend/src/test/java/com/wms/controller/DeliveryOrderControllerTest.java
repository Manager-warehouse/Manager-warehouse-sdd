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
import com.wms.dto.response.DeliveryOrderResponse;
import com.wms.entity.User;
import com.wms.enums.DeliveryOrderStatus;
import com.wms.enums.DeliveryOrderType;
import com.wms.enums.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.exception.OutboundDeliveryException;
import com.wms.service.CurrentUserService;
import com.wms.service.DeliveryOrderService;
import com.wms.util.JwtUtil;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DeliveryOrderController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class DeliveryOrderControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private DeliveryOrderService deliveryOrderService;
    @MockBean private CurrentUserService currentUserService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private User planner;
    private User manager;

    @BeforeEach
    void setUp() {
        planner = user(1L, UserRole.PLANNER);
        manager = user(2L, UserRole.WAREHOUSE_MANAGER);
    }

    @Test
    @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
    void createDeliveryOrder_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(planner);
        when(deliveryOrderService.createDeliveryOrder(any(), eq(planner))).thenReturn(response(DeliveryOrderStatus.NEW));

        mockMvc.perform(post("/api/v1/delivery-orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.dealerId").value(10))
                .andExpect(jsonPath("$.warehouseId").value(20));
    }

    @Test
    @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
    void createDeliveryOrder_rejectsBusinessError() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(planner);
        when(deliveryOrderService.createDeliveryOrder(any(), eq(planner)))
                .thenThrow(new OutboundDeliveryException("INSUFFICIENT_STOCK",
                        HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient stock"));

        mockMvc.perform(post("/api/v1/delivery-orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    @Test
    @WithMockUser(username = "manager@wms.com", roles = "WAREHOUSE_MANAGER")
    void cancelDeliveryOrder_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(manager);
        when(deliveryOrderService.cancelDeliveryOrder(eq(100L), any(), eq(manager)))
                .thenReturn(response(DeliveryOrderStatus.CANCELLED));

        mockMvc.perform(put("/api/v1/delivery-orders/100/cancel")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cancelReason\":\"Customer changed order\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
    void cancelDeliveryOrder_rejectsNonManagerRole() throws Exception {
        mockMvc.perform(put("/api/v1/delivery-orders/100/cancel")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cancelReason\":\"Customer changed order\"}"))
                .andExpect(status().isForbidden());
    }

    private String createJson() {
        return """
                {
                  "dealerId": 10,
                  "warehouseId": 20,
                  "type": "SALE",
                  "documentDate": "2026-06-18",
                  "expectedDeliveryDate": "2026-06-20",
                  "items": [
                    {"productId": 30, "requestedQty": 10}
                  ]
                }
                """;
    }

    private DeliveryOrderResponse response(DeliveryOrderStatus status) {
        return DeliveryOrderResponse.builder()
                .id(100L)
                .doNumber("DO-20260618-0001")
                .dealerId(10L)
                .warehouseId(20L)
                .type(DeliveryOrderType.SALE)
                .documentDate(LocalDate.of(2026, 6, 18))
                .expectedDeliveryDate(LocalDate.of(2026, 6, 20))
                .status(status)
                .items(List.of())
                .build();
    }

    private User user(Long id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setFullName(role.name());
        return user;
    }
}
