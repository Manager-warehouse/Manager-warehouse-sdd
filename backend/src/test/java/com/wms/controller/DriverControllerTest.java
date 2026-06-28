package com.wms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.dto.request.DriverRequest;
import com.wms.dto.response.DriverResponse;
import com.wms.dto.response.UserResponse;
import com.wms.entity.User;
import com.wms.enums.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.repository.UserRepository;
import com.wms.service.DriverService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DriverController.class)
@Import({ SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class })
public class DriverControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DriverService driverService;
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
    void getAllDrivers_Dispatcher_Returns200() throws Exception {
        when(userRepository.findByEmail("dispatcher@wms.com")).thenReturn(Optional.of(dispatcherUser));
        when(driverService.getAllDrivers(any(), any(), eq(4L))).thenReturn(List.of(new DriverResponse()));

        mockMvc.perform(get("/api/v1/dispatcher/drivers"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllDrivers_Unauthenticated_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/dispatcher/drivers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "dispatcher@wms.com", roles = "DISPATCHER")
    void getDriverUserCandidates_Dispatcher_Returns200() throws Exception {
        when(userRepository.findByEmail("dispatcher@wms.com")).thenReturn(Optional.of(dispatcherUser));
        when(driverService.getDriverUserCandidates(4L)).thenReturn(List.of(new UserResponse()));

        mockMvc.perform(get("/api/v1/dispatcher/drivers/candidate-users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "dispatcher@wms.com", roles = "DISPATCHER")
    void createDriver_Dispatcher_Success() throws Exception {
        when(userRepository.findByEmail("dispatcher@wms.com")).thenReturn(Optional.of(dispatcherUser));
        when(driverService.createDriver(any(), eq(4L))).thenReturn(new DriverResponse());

        DriverRequest req = new DriverRequest();
        req.setWarehouseId(2L);
        req.setUserId(3L);
        req.setFullName("Nguyen Van A");
        req.setLicenseNumber("LX-12345");
        req.setLicenseExpiry(LocalDate.now().plusYears(5));

        mockMvc.perform(post("/api/v1/dispatcher/drivers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "dispatcher@wms.com", roles = "DISPATCHER")
    void createDriver_DuplicateLicense_Returns409() throws Exception {
        when(userRepository.findByEmail("dispatcher@wms.com")).thenReturn(Optional.of(dispatcherUser));
        when(driverService.createDriver(any(), eq(4L)))
                .thenThrow(new IllegalArgumentException("DUPLICATE_LICENSE_NUMBER"));

        DriverRequest req = new DriverRequest();
        req.setWarehouseId(2L);
        req.setUserId(3L);
        req.setFullName("Nguyen Van A");
        req.setLicenseNumber("LX-12345");
        req.setLicenseExpiry(LocalDate.now().plusYears(5));

        mockMvc.perform(post("/api/v1/dispatcher/drivers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }
}


