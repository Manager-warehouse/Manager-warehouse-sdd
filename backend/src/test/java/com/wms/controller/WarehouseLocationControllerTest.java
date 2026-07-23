package com.wms.controller;


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
import com.wms.controller.user_configuration.*;
import com.wms.controller.audit_trail.*;
import com.wms.controller.access_control.*;
import com.wms.controller.billing_payment.*;
import com.wms.controller.stock_receiving.*;
import com.wms.controller.stock_control.*;
import com.wms.controller.notification_delivery.*;
import com.wms.controller.order_fulfillment.*;
import com.wms.controller.price_management.*;
import com.wms.controller.reporting_alerting.*;
import com.wms.controller.return_disposal.*;
import com.wms.controller.stock_counting.*;
import com.wms.controller.fleet_management.*;
import com.wms.controller.warehouse_location.*;
import com.wms.controller.warehouse_transfer.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.dto.request.WarehouseLocationRequest;
import com.wms.dto.response.WarehouseLocationResponse;
import com.wms.entity.access_control.User;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.repository.UserRepository;
import com.wms.service.warehouse_location.WarehouseLocationService;
import com.wms.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WarehouseLocationController.class)
@Import({ SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class })
public class WarehouseLocationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WarehouseLocationService locationService;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    private User managerUser;

    @BeforeEach
    void setUp() {
        managerUser = new User();
        managerUser.setId(2L);
        managerUser.setEmail("manager@wms.com");
        managerUser.setRole(UserRole.WAREHOUSE_MANAGER);
    }

    @Test
    @WithMockUser(username = "manager@wms.com", roles = "WAREHOUSE_MANAGER")
    void getAllLocations_Manager_Returns200() throws Exception {
        when(locationService.getAllLocations(any(), any(), any(), any(), any()))
                .thenReturn(List.of(new WarehouseLocationResponse()));

        mockMvc.perform(get("/api/v1/admin/warehouse-locations"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllLocations_Unauthenticated_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/warehouse-locations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "manager@wms.com", roles = "WAREHOUSE_MANAGER")
    void createLocation_Manager_Success() throws Exception {
        when(userRepository.findByEmail("manager@wms.com")).thenReturn(Optional.of(managerUser));
        when(locationService.createLocation(any(), eq(2L))).thenReturn(new WarehouseLocationResponse());

        WarehouseLocationRequest req = new WarehouseLocationRequest();
        req.setWarehouseId(10L);
        req.setCode("A");
        req.setType("ZONE");

        mockMvc.perform(post("/api/v1/admin/warehouse-locations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "keeper@wms.com", roles = "STOREKEEPER")
    void createLocation_Storekeeper_Returns403() throws Exception {
        WarehouseLocationRequest req = new WarehouseLocationRequest();
        req.setWarehouseId(10L);
        req.setCode("A");
        req.setType("ZONE");

        mockMvc.perform(post("/api/v1/admin/warehouse-locations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager@wms.com", roles = "WAREHOUSE_MANAGER")
    void updateLocation_OverCapacity_Returns422() throws Exception {
        when(userRepository.findByEmail("manager@wms.com")).thenReturn(Optional.of(managerUser));
        when(locationService.updateLocation(eq(30L), any(), eq(2L)))
                .thenThrow(new IllegalArgumentException("BIN_OVER_CAPACITY"));

        WarehouseLocationRequest req = new WarehouseLocationRequest();
        req.setWarehouseId(10L);
        req.setCode("BIN01");
        req.setType("BIN");
        req.setParentId(20L);

        mockMvc.perform(put("/api/v1/admin/warehouse-locations/30")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity());
    }
}


