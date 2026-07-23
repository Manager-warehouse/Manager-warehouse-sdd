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
import com.wms.dto.request.UserRequest;
import com.wms.dto.request.UserStatusRequest;
import com.wms.dto.response.UserResponse;
import com.wms.entity.access_control.User;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.UserRepository;
import com.wms.service.user_configuration.UserService;
import com.wms.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({ SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class })
class UserControllerTest {

        @Autowired
        private MockMvc mockMvc;
        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
    private UserService userService;
        @MockBean
    private UserRepository userRepository;
        @MockBean
    private JwtUtil jwtUtil;
        @MockBean
    private UserDetailsServiceImpl userDetailsService;

        private User adminUser;
        private UserResponse userResponse;

        @BeforeEach
        void setUp() {
                adminUser = new User();
                adminUser.setId(1L);
                adminUser.setEmail("admin@phucanh.vn");
                adminUser.setRole(UserRole.ADMIN);
                adminUser.setFullName("System Admin");

                userResponse = UserResponse.builder()
                                .id(2L)
                                .code("USR01")
                                .fullName("John Doe")
                                .email("john@phucanh.vn")
                                .role(UserRole.STOREKEEPER)
                                .isActive(true)
                                .warehouses(List.of(10L))
                                .build();
        }

        @Test
        @DisplayName("GET /api/v1/admin/users — 200 OK khi ADMIN truy cập")
        @WithMockUser(username = "admin@phucanh.vn", roles = "ADMIN")
        void getAllUsers_admin_returns200() throws Exception {
                when(userService.getAllUsers()).thenReturn(List.of(userResponse));

                mockMvc.perform(get("/api/v1/admin/users").contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].email").value("john@phucanh.vn"));
        }

        @Test
        @DisplayName("GET /api/v1/admin/users — 403 FORBIDDEN khi WAREHOUSE_MANAGER truy cập")
        @WithMockUser(username = "manager@phucanh.vn", roles = "WAREHOUSE_MANAGER")
        void getAllUsers_manager_returns403() throws Exception {
                mockMvc.perform(get("/api/v1/admin/users").contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /api/v1/admin/users/{id} — 200 OK khi ADMIN truy cập")
        @WithMockUser(username = "admin@phucanh.vn", roles = "ADMIN")
        void getUserById_admin_returns200() throws Exception {
                when(userService.getUserById(2L)).thenReturn(userResponse);

                mockMvc.perform(get("/api/v1/admin/users/2").contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("john@phucanh.vn"));
        }

        @Test
        @DisplayName("GET /api/v1/admin/users/{id} — 404 NOT FOUND khi user không tồn tại")
        @WithMockUser(username = "admin@phucanh.vn", roles = "ADMIN")
        void getUserById_notFound_returns404() throws Exception {
                when(userService.getUserById(99L)).thenThrow(new ResourceNotFoundException("User not found"));

                mockMvc.perform(get("/api/v1/admin/users/99").contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("POST /api/v1/admin/users — 201 CREATED khi ADMIN tạo hợp lệ")
        @WithMockUser(username = "admin@phucanh.vn", roles = "ADMIN")
        void createUser_admin_validPayload_returns201() throws Exception {
                when(userRepository.findByEmail("admin@phucanh.vn")).thenReturn(Optional.of(adminUser));
                when(userService.createUser(any(UserRequest.class), eq(1L))).thenReturn(userResponse);

                UserRequest request = UserRequest.builder()
                                .code("USR01")
                                .fullName("John Doe")
                                .email("john@phucanh.vn")
                                .password("Password123")
                                .role(UserRole.STOREKEEPER)
                                .warehouses(List.of(10L))
                                .build();

                mockMvc.perform(post("/api/v1/admin/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.email").value("john@phucanh.vn"));
        }

        @Test
        @DisplayName("POST /api/v1/admin/users — 400 BAD REQUEST khi email không hợp lệ")
        @WithMockUser(username = "admin@phucanh.vn", roles = "ADMIN")
        void createUser_invalidEmail_returns400() throws Exception {
                UserRequest request = UserRequest.builder()
                                .code("USR01")
                                .fullName("John Doe")
                                .email("invalid-email")
                                .password("Password123")
                                .role(UserRole.STOREKEEPER)
                                .build();

                mockMvc.perform(post("/api/v1/admin/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("email: Invalid email format"));
        }

        @Test
        @DisplayName("PUT /api/v1/admin/users/{id} — 200 OK khi ADMIN cập nhật")
        @WithMockUser(username = "admin@phucanh.vn", roles = "ADMIN")
        void updateUser_admin_returns200() throws Exception {
                when(userRepository.findByEmail("admin@phucanh.vn")).thenReturn(Optional.of(adminUser));
                when(userService.updateUser(eq(2L), any(UserRequest.class), eq(1L))).thenReturn(userResponse);

                UserRequest request = UserRequest.builder()
                                .code("USR01")
                                .fullName("John Doe")
                                .email("john@phucanh.vn")
                                .role(UserRole.STOREKEEPER)
                                .build();

                mockMvc.perform(put("/api/v1/admin/users/2")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("john@phucanh.vn"));
        }

        @Test
        @DisplayName("DELETE /api/v1/admin/users/{id} — 200 OK khi ADMIN xóa")
        @WithMockUser(username = "admin@phucanh.vn", roles = "ADMIN")
        void softDeleteUser_admin_returns200() throws Exception {
                when(userRepository.findByEmail("admin@phucanh.vn")).thenReturn(Optional.of(adminUser));
                when(userService.softDeleteUser(eq(2L), eq(1L))).thenReturn(userResponse);

                mockMvc.perform(delete("/api/v1/admin/users/2").contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PUT /api/v1/admin/users/{id}/status — 200 OK khi ADMIN đổi trạng thái")
        @WithMockUser(username = "admin@phucanh.vn", roles = "ADMIN")
        void toggleUserStatus_admin_returns200() throws Exception {
                when(userRepository.findByEmail("admin@phucanh.vn")).thenReturn(Optional.of(adminUser));
                when(userService.toggleUserStatus(eq(2L), eq(false), eq(1L))).thenReturn(userResponse);

                UserStatusRequest request = UserStatusRequest.builder().isActive(false).build();

                mockMvc.perform(put("/api/v1/admin/users/2/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }
}


