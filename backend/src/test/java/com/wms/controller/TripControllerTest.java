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
import com.wms.dto.response.TripDriverViewResponse;
import com.wms.dto.response.TripDeliveryOrderResponse;
import com.wms.dto.response.TripResponse;
import com.wms.entity.User;
import com.wms.enums.DeliveryOrderStatus;
import com.wms.enums.TripStatus;
import com.wms.enums.TripType;
import com.wms.enums.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.exception.OutboundDeliveryException;
import com.wms.service.CurrentUserService;
import com.wms.service.DriverDeliveryService;
import com.wms.service.TripService;
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

@WebMvcTest(TripController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class TripControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private TripService tripService;
    @MockBean private DriverDeliveryService driverDeliveryService;
    @MockBean private CurrentUserService currentUserService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private User dispatcher;
    private User driver;

    @BeforeEach
    void setUp() {
        dispatcher = user(1L, UserRole.DISPATCHER);
        driver = user(2L, UserRole.DRIVER);
    }

    @Test
    @WithMockUser(username = "dispatcher@wms.com", roles = "DISPATCHER")
    void createTrip_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(dispatcher);
        when(tripService.createTrip(any(), eq(dispatcher))).thenReturn(response(TripStatus.PLANNED));

        mockMvc.perform(post("/api/v1/trips")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.deliveryOrders[0].stopOrder").value(1));
    }

    @Test
    @WithMockUser(username = "dispatcher@wms.com", roles = "DISPATCHER")
    void updateTrip_rejectsBusinessError() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(dispatcher);
        when(tripService.updateTrip(eq(900L), any(), eq(dispatcher)))
                .thenThrow(new OutboundDeliveryException("TRIP_NOT_EDITABLE",
                        HttpStatus.UNPROCESSABLE_ENTITY, "Trip is no longer editable"));

        mockMvc.perform(put("/api/v1/trips/900")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TRIP_NOT_EDITABLE"));
    }

    @Test
    @WithMockUser(username = "dispatcher@wms.com", roles = "DISPATCHER")
    void cancelTrip_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(dispatcher);
        when(tripService.cancelTrip(eq(900L), any(), eq(dispatcher))).thenReturn(response(TripStatus.CANCELLED));

        mockMvc.perform(put("/api/v1/trips/900/cancel")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Dealer postponed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser(username = "driver@wms.com", roles = "DRIVER")
    void departTrip_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(driver);
        when(tripService.departTrip(eq(900L), any(), eq(driver))).thenReturn(response(TripStatus.IN_TRANSIT));

        mockMvc.perform(put("/api/v1/trips/900/depart")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Loaded\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"));
    }

    @Test
    @WithMockUser(username = "driver@wms.com", roles = "DRIVER")
    void departTrip_rejectsDriverScopeFailure() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(driver);
        when(tripService.departTrip(eq(900L), any(), eq(driver)))
                .thenThrow(new OutboundDeliveryException("TRIP_DRIVER_SCOPE_FORBIDDEN",
                        HttpStatus.FORBIDDEN, "Authenticated driver is not assigned to this trip"));

        mockMvc.perform(put("/api/v1/trips/900/depart")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Wrong driver\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TRIP_DRIVER_SCOPE_FORBIDDEN"));
    }

    @Test
    @WithMockUser(username = "driver@wms.com", roles = "DRIVER")
    void completeTrip_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(driver);
        when(driverDeliveryService.completeTrip(eq(900L), any(), eq(driver)))
                .thenReturn(driverTripResponse(TripStatus.COMPLETED));

        mockMvc.perform(put("/api/v1/trips/900/complete")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Returned\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(900))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(username = "driver@wms.com", roles = "DRIVER")
    void completeTrip_rejectsReadinessFailure() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(driver);
        when(driverDeliveryService.completeTrip(eq(900L), any(), eq(driver)))
                .thenThrow(new OutboundDeliveryException("TRIP_NOT_READY_TO_COMPLETE",
                        HttpStatus.UNPROCESSABLE_ENTITY, "All delivery orders must be terminal"));

        mockMvc.perform(put("/api/v1/trips/900/complete")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Returned\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TRIP_NOT_READY_TO_COMPLETE"));
    }

    private TripDriverViewResponse driverTripResponse(TripStatus status) {
        return TripDriverViewResponse.builder()
                .tripId(900L)
                .tripNumber("TRIP-20260620-0001")
                .status(status)
                .driverId(401L)
                .vehicleId(301L)
                .deliveryOrders(List.of())
                .build();
    }

    private String createJson() {
        return """
                {
                  "warehouseId": 20,
                  "vehicleId": 301,
                  "driverId": 401,
                  "plannedDate": "2026-06-22",
                  "deliveryOrders": [
                    {"doId": 101, "stopOrder": 1}
                  ]
                }
                """;
    }

    private String updateJson() {
        return """
                {
                  "vehicleId": 301,
                  "driverId": 401,
                  "plannedDate": "2026-06-22",
                  "deliveryOrders": [
                    {"doId": 101, "stopOrder": 1}
                  ]
                }
                """;
    }

    private TripResponse response(TripStatus status) {
        return TripResponse.builder()
                .id(900L)
                .tripNumber("TRIP-20260620-0001")
                .warehouseId(20L)
                .vehicleId(301L)
                .driverId(401L)
                .dispatcherId(1L)
                .plannedDate(LocalDate.of(2026, 6, 22))
                .tripType(TripType.DELIVERY)
                .status(status)
                .deliveryOrders(List.of(TripDeliveryOrderResponse.builder()
                        .doId(101L)
                        .doNumber("DO-101")
                        .warehouseId(20L)
                        .status(DeliveryOrderStatus.WAREHOUSE_APPROVED)
                        .stopOrder(1)
                        .build()))
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
