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
import com.wms.dto.request.VehicleRequest;
import com.wms.dto.response.VehicleResponse;
import com.wms.entity.access_control.User;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.repository.UserRepository;
import com.wms.service.fleet_management.VehicleService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VehicleController.class)
@Import({ SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class })
public class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VehicleService vehicleService;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    private User dispatcherUser;

    @BeforeEach
    void setUp() {
        dispatcherUser = new User();
        dispatcherUser.setId(4L);
        dispatcherUser.setEmail("dispatcher@wms.com");
        dispatcherUser.setRole(UserRole.DISPATCHER);
    }

    @Test
    @WithMockUser(username = "dispatcher@wms.com", roles = "DISPATCHER")
    void getAllVehicles_Dispatcher_Returns200() throws Exception {
        when(vehicleService.getAllVehicles(any(), any())).thenReturn(List.of(new VehicleResponse()));

        mockMvc.perform(get("/api/v1/dispatcher/vehicles"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllVehicles_Unauthenticated_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/dispatcher/vehicles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "dispatcher@wms.com", roles = "DISPATCHER")
    void createVehicle_Dispatcher_Success() throws Exception {
        when(userRepository.findByEmail("dispatcher@wms.com")).thenReturn(Optional.of(dispatcherUser));
        when(vehicleService.createVehicle(any(), eq(4L))).thenReturn(new VehicleResponse());

        VehicleRequest req = new VehicleRequest();
        req.setWarehouseId(2L);
        req.setPlateNumber("29C-12345");
        req.setVehicleType("Container");
        req.setMaxWeightKg(BigDecimal.valueOf(10000.0));
        req.setWarehouseId(1L);

        mockMvc.perform(post("/api/v1/dispatcher/vehicles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
    void createVehicle_Planner_Returns403() throws Exception {
        VehicleRequest req = new VehicleRequest();
        req.setWarehouseId(2L);
        req.setPlateNumber("29C-12345");
        req.setVehicleType("Container");
        req.setMaxWeightKg(BigDecimal.valueOf(10000.0));
        req.setWarehouseId(1L);

        mockMvc.perform(post("/api/v1/dispatcher/vehicles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dispatcher@wms.com", roles = "DISPATCHER")
    void createVehicle_DuplicatePlate_Returns409() throws Exception {
        when(userRepository.findByEmail("dispatcher@wms.com")).thenReturn(Optional.of(dispatcherUser));
        when(vehicleService.createVehicle(any(), eq(4L)))
                .thenThrow(new IllegalArgumentException("DUPLICATE_PLATE_NUMBER"));

        VehicleRequest req = new VehicleRequest();
        req.setWarehouseId(2L);
        req.setPlateNumber("29C-12345");
        req.setVehicleType("Container");
        req.setMaxWeightKg(BigDecimal.valueOf(10000.0));
        req.setWarehouseId(1L);

        mockMvc.perform(post("/api/v1/dispatcher/vehicles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }
}


