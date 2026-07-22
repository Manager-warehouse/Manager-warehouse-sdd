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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.wms.entity.access_control.User;
import com.wms.enums.order_fulfillment.DeliveryOrderStatus;
import com.wms.enums.order_fulfillment.TripStatus;
import com.wms.enums.order_fulfillment.TripType;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.exception.OutboundDeliveryException;
import com.wms.service.user_context.CurrentUserService;
import com.wms.service.order_fulfillment.DriverDeliveryService;
import com.wms.service.order_fulfillment.TripService;
import com.wms.util.JwtUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    void listTrips_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(dispatcher);
        when(tripService.listTrips(eq(20L), eq(TripStatus.PLANNED), eq(dispatcher)))
                .thenReturn(List.of(response(TripStatus.PLANNED)));

        mockMvc.perform(get("/api/v1/trips")
                        .param("warehouseId", "20")
                        .param("status", "PLANNED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tripNumber").value("TRIP-20260620-0001"))
                .andExpect(jsonPath("$[0].status").value("PLANNED"));
    }

    @Test
    @WithMockUser(username = "driver@wms.com", roles = "DRIVER")
    void listDriverTrips_returnsOperationalTripFields() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(driver);
        when(driverDeliveryService.listMyTrips(driver))
                .thenReturn(List.of(driverTripResponse(TripStatus.PLANNED)));

        mockMvc.perform(get("/api/v1/trips/driver"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tripId").value(900))
                .andExpect(jsonPath("$[0].vehiclePlate").value("36C-88888"))
                .andExpect(jsonPath("$[0].driverName").value("Driver Test 2"))
                .andExpect(jsonPath("$[0].plannedStartAt").value("2026-07-17T14:06:00"))
                .andExpect(jsonPath("$[0].plannedEndAt").value("2026-07-26T14:06:00"))
                .andExpect(jsonPath("$[0].totalWeightKg").value(25.50));
    }

    @Test
    @WithMockUser(username = "dispatcher@wms.com", roles = "DISPATCHER")
    void getTrip_dispatcherReturnsVehicleAndDriverDetails() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(dispatcher);
        when(tripService.getTrip(900L, dispatcher)).thenReturn(response(TripStatus.PLANNED));

        mockMvc.perform(get("/api/v1/trips/900"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehiclePlate").value("36C-88888"))
                .andExpect(jsonPath("$.driverName").value("Driver Test 2"))
                .andExpect(jsonPath("$.deliveryOrders[0].doNumber").value("DO-101"));
    }

    @Test
    @WithMockUser(username = "driver@wms.com", roles = "DRIVER")
    void getDriverTrip_usesDriverScopedDetailRoute() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(driver);
        when(driverDeliveryService.getAssignedTrip(900L, driver))
                .thenReturn(driverTripResponse(TripStatus.PLANNED));

        mockMvc.perform(get("/api/v1/trips/driver/900"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(900))
                .andExpect(jsonPath("$.vehiclePlate").value("36C-88888"));
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
    void createTrip_returnsBadRequestForDuplicateDeliveryOrderId() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(dispatcher);
        when(tripService.createTrip(any(), eq(dispatcher)))
                .thenThrow(new OutboundDeliveryException("DUPLICATE_DELIVERY_ORDER_ID",
                        HttpStatus.BAD_REQUEST, "Duplicate Delivery Order ID(s) found in request: [101]"));

        mockMvc.perform(post("/api/v1/trips")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DUPLICATE_DELIVERY_ORDER_ID"))
                .andExpect(jsonPath("$.message").value("Duplicate Delivery Order ID(s) found in request: [101]"));
    }

    @Test
    @WithMockUser(username = "dispatcher@wms.com", roles = "DISPATCHER")
    void createTrip_returnsConflictForAlreadyAssignedDeliveryOrder() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(dispatcher);
        when(tripService.createTrip(any(), eq(dispatcher)))
                .thenThrow(new OutboundDeliveryException("DELIVERY_ORDER_ALREADY_ASSIGNED",
                        HttpStatus.CONFLICT, "Delivery Order 7 is already assigned to trip 3"));

        mockMvc.perform(post("/api/v1/trips")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DELIVERY_ORDER_ALREADY_ASSIGNED"))
                .andExpect(jsonPath("$.message").value("Delivery Order 7 is already assigned to trip 3"));
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
                .driverName("Driver Test 2")
                .vehicleId(301L)
                .vehiclePlate("36C-88888")
                .plannedDate(LocalDate.of(2026, 7, 17))
                .plannedStartAt(LocalDateTime.of(2026, 7, 17, 14, 6))
                .plannedEndAt(LocalDateTime.of(2026, 7, 26, 14, 6))
                .totalWeightKg(new BigDecimal("25.50"))
                .totalVolumeM3(new BigDecimal("1.250"))
                .deliveryOrders(List.of())
                .build();
    }

    private String createJson() {
        return """
                {
                  "warehouseId": 20,
                  "vehicleId": 301,
                  "driverId": 401,
                  "plannedStartAt": "2026-06-22T08:00:00",
                  "plannedEndAt": "2026-06-22T17:00:00",
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
                  "plannedStartAt": "2026-06-22T08:00:00",
                  "plannedEndAt": "2026-06-22T17:00:00",
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
                .vehiclePlate("36C-88888")
                .vehicleType("Xe tai")
                .vehicleMaxWeightKg(BigDecimal.valueOf(3500))
                .vehicleMaxVolumeM3(BigDecimal.valueOf(18))
                .driverId(401L)
                .driverName("Driver Test 2")
                .driverPhone("0900000000")
                .driverLicenseNumber("GPLX-001")
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
