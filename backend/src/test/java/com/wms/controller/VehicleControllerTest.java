package com.wms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.dto.request.VehicleRequest;
import com.wms.dto.response.VehicleResponse;
import com.wms.entity.User;
import com.wms.enums.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.repository.UserRepository;
import com.wms.service.VehicleService;
import com.wms.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
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

    @Mock
    private VehicleService vehicleService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
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
