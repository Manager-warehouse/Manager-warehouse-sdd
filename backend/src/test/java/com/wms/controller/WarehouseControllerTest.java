package com.wms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.dto.request.WarehouseRequest;
import com.wms.dto.response.WarehouseResponse;
import com.wms.entity.User;
import com.wms.enums.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.repository.UserRepository;
import com.wms.service.WarehouseService;
import com.wms.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

@WebMvcTest(WarehouseController.class)
@Import({ SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class })
public class WarehouseControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WarehouseService warehouseService;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setEmail("admin@wms.com");
        adminUser.setRole(UserRole.ADMIN);
    }

    @Test
    @WithMockUser(username = "admin@wms.com", roles = "ADMIN")
    void getAllWarehouses_Admin_Returns200() throws Exception {
        when(userRepository.findByEmail("admin@wms.com")).thenReturn(Optional.of(adminUser));
        when(warehouseService.getAllWarehouses(any(), eq(1L))).thenReturn(List.of(new WarehouseResponse()));

        mockMvc.perform(get("/api/v1/admin/warehouses"))
                .andExpect(status().isOk());
    }


    @Test
    void getAllWarehouses_Unauthenticated_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/warehouses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@wms.com", roles = "ADMIN")
    void createWarehouse_Admin_Success() throws Exception {
        when(userRepository.findByEmail("admin@wms.com")).thenReturn(Optional.of(adminUser));
        when(warehouseService.createWarehouse(any(), eq(1L))).thenReturn(new WarehouseResponse());

        WarehouseRequest req = new WarehouseRequest();
        req.setCode("WH-HP");
        req.setName("Hai Phong");
        req.setAddress("Hai Phong, Vietnam");
        req.setPhone("0901234567");
        req.setType("PHYSICAL");

        mockMvc.perform(post("/api/v1/admin/warehouses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "manager@wms.com", roles = "WAREHOUSE_MANAGER")
    void createWarehouse_Manager_Returns403() throws Exception {
        WarehouseRequest req = new WarehouseRequest();
        req.setCode("WH-HP");
        req.setName("Hai Phong");
        req.setAddress("Hai Phong, Vietnam");
        req.setType("PHYSICAL");

        mockMvc.perform(post("/api/v1/admin/warehouses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@wms.com", roles = "ADMIN")
    void createWarehouse_DuplicateCode_Returns409() throws Exception {
        when(userRepository.findByEmail("admin@wms.com")).thenReturn(Optional.of(adminUser));
        when(warehouseService.createWarehouse(any(), eq(1L)))
                .thenThrow(new IllegalArgumentException("DUPLICATE_WAREHOUSE_CODE"));

        WarehouseRequest req = new WarehouseRequest();
        req.setCode("WH-HP");
        req.setName("Hai Phong");
        req.setAddress("Hai Phong, Vietnam");
        req.setType("PHYSICAL");

        mockMvc.perform(post("/api/v1/admin/warehouses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }
}


