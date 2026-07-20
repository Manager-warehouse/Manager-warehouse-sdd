package com.wms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.dto.response.DeliveryAttemptResponse;
import com.wms.dto.response.DeliveryOtpResponse;
import com.wms.dto.response.DriverDeliveryOrderResponse;
import com.wms.dto.response.TripDriverViewResponse;
import com.wms.entity.User;
import com.wms.enums.DeliveryOrderStatus;
import com.wms.enums.DeliveryOtpStatus;
import com.wms.enums.DeliveryStatus;
import com.wms.enums.TripStatus;
import com.wms.enums.TripType;
import com.wms.enums.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.exception.OutboundDeliveryException;
import com.wms.service.CurrentUserService;
import com.wms.service.DriverDeliveryService;
import com.wms.service.TripService;
import com.wms.util.JwtUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TripController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class DriverDeliveryControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private TripService tripService;
    @MockBean private DriverDeliveryService driverDeliveryService;
    @MockBean private CurrentUserService currentUserService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private User driver;

    @BeforeEach
    void setUp() {
        driver = new User();
        driver.setId(2L);
        driver.setRole(UserRole.DRIVER);
        driver.setFullName("Driver");
    }

    @Test
    @WithMockUser(username = "driver@wms.com", roles = "DRIVER")
    void getAssignedTrip_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(driver);
        when(driverDeliveryService.getAssignedTrip(900L, driver)).thenReturn(tripView());

        mockMvc.perform(get("/api/v1/trips/900"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(900))
                .andExpect(jsonPath("$.vehiclePlate").value("36C-88888"))
                .andExpect(jsonPath("$.driverName").value("Driver Test 2"))
                .andExpect(jsonPath("$.plannedStartAt").value("2026-07-17T14:06:00"))
                .andExpect(jsonPath("$.deliveryOrders[0].currentAttempt.status").value("IN_TRANSIT"));
    }

    @Test
    @WithMockUser(username = "driver@wms.com", roles = "DRIVER")
    void listDriverTrips_returnsMixedDeliveryAndTransferTripsForAuthenticatedDriver() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(driver);
        when(driverDeliveryService.listMyTrips(driver)).thenReturn(List.of(tripView(), transferTripView()));

        mockMvc.perform(get("/api/v1/trips/driver"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tripType").value("DELIVERY"))
                .andExpect(jsonPath("$[0].tripTypeLabel").value("Giao dai ly"))
                .andExpect(jsonPath("$[0].deliveryStopCount").value(1))
                .andExpect(jsonPath("$[1].tripType").value("TRANSFER"))
                .andExpect(jsonPath("$[1].tripTypeLabel").value("Dieu chuyen noi bo"))
                .andExpect(jsonPath("$[1].transferId").value(500))
                .andExpect(jsonPath("$[1].sourceWarehouseCode").value("HP"))
                .andExpect(jsonPath("$[1].destinationWarehouseCode").value("HN"))
                .andExpect(jsonPath("$[1].transferLineCount").value(2))
                .andExpect(jsonPath("$[1].deliveryOrders").isEmpty());
    }

    @Test
    @WithMockUser(username = "driver@wms.com", roles = "DRIVER")
    void uploadPodEvidence_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(driver);
        when(driverDeliveryService.uploadPodEvidence(eq(900L), eq(101L), any(), any(), any(), eq(driver)))
                .thenReturn(attempt(DeliveryStatus.IN_TRANSIT));

        MockMultipartFile goods = new MockMultipartFile("goodsImage", "goods.jpg", "image/jpeg", "ok".getBytes());
        MockMultipartFile sign = new MockMultipartFile("signDocumentImage", "sign.jpg", "image/jpeg", "ok".getBytes());

        mockMvc.perform(multipart("/api/v1/trips/900/delivery-orders/101/pod-evidence")
                        .file(goods)
                        .file(sign)
                        .param("notes", "signed")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryId").value(700));
    }

    @Test
    @WithMockUser(username = "driver@wms.com", roles = "DRIVER")
    void requestDeliveryOtp_rejectsMissingPod() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(driver);
        when(driverDeliveryService.requestDeliveryOtp(eq(900L), eq(101L), any(), eq(driver)))
                .thenThrow(new OutboundDeliveryException("MISSING_POD", HttpStatus.BAD_REQUEST, "Missing POD"));

        mockMvc.perform(post("/api/v1/trips/900/delivery-orders/101/delivery-otp")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resend\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_POD"));
    }

    @Test
    @WithMockUser(username = "driver@wms.com", roles = "DRIVER")
    void requestDeliveryOtp_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(driver);
        when(driverDeliveryService.requestDeliveryOtp(eq(900L), eq(101L), any(), eq(driver)))
                .thenReturn(otp(DeliveryOtpStatus.ACTIVE));

        mockMvc.perform(post("/api/v1/trips/900/delivery-orders/101/delivery-otp")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resend\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.recipientEmail").value("dealer@example.com"));
    }

    @Test
    @WithMockUser(username = "driver@wms.com", roles = "DRIVER")
    void requestDeliveryOtp_rejectsActiveOtpConflict() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(driver);
        when(driverDeliveryService.requestDeliveryOtp(eq(900L), eq(101L), any(), eq(driver)))
                .thenThrow(new OutboundDeliveryException("OTP_STILL_ACTIVE", HttpStatus.CONFLICT, "OTP still active"));

        mockMvc.perform(post("/api/v1/trips/900/delivery-orders/101/delivery-otp")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resend\":true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OTP_STILL_ACTIVE"));
    }

    @Test
    @WithMockUser(username = "driver@wms.com", roles = "DRIVER")
    void confirmDelivery_rejectsInvalidOtp() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(driver);
        when(driverDeliveryService.confirmDelivery(eq(900L), eq(101L), any(), eq(driver)))
                .thenThrow(new OutboundDeliveryException("DELIVERY_OTP_INVALID", HttpStatus.BAD_REQUEST, "Invalid OTP"));

        mockMvc.perform(put("/api/v1/trips/900/delivery-orders/101/confirm-delivery")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DELIVERY_OTP_INVALID"));
    }

    @Test
    @WithMockUser(username = "driver@wms.com", roles = "DRIVER")
    void confirmDelivery_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(driver);
        when(driverDeliveryService.confirmDelivery(eq(900L), eq(101L), any(), eq(driver)))
                .thenReturn(attempt(DeliveryStatus.DELIVERED));

        mockMvc.perform(put("/api/v1/trips/900/delivery-orders/101/confirm-delivery")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    @WithMockUser(username = "driver@wms.com", roles = "DRIVER")
    void failDelivery_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(driver);
        when(driverDeliveryService.failDelivery(eq(900L), eq(101L), any(), eq(driver)))
                .thenReturn(attempt(DeliveryStatus.FAILED));

        mockMvc.perform(put("/api/v1/trips/900/delivery-orders/101/fail-delivery")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"failureReason\":\"Dealer refused\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    @WithMockUser(username = "driver@wms.com", roles = "DRIVER")
    void completeTrip_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(driver);
        when(driverDeliveryService.completeTrip(eq(900L), any(), eq(driver)))
                .thenReturn(tripView(TripStatus.COMPLETED));

        mockMvc.perform(put("/api/v1/trips/900/complete")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Vehicle returned\"}"))
                .andExpect(status().isOk())
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
                        .content("{\"notes\":\"Vehicle returned\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TRIP_NOT_READY_TO_COMPLETE"));
    }

    private TripDriverViewResponse tripView() {
        return tripView(TripStatus.IN_TRANSIT);
    }

    private TripDriverViewResponse tripView(TripStatus status) {
        return TripDriverViewResponse.builder()
                .tripId(900L)
                .tripNumber("TRIP-1")
                .status(status)
                .tripType(TripType.DELIVERY)
                .tripTypeLabel("Giao dai ly")
                .driverId(401L)
                .driverName("Driver Test 2")
                .vehicleId(301L)
                .vehiclePlate("36C-88888")
                .plannedDate(LocalDate.of(2026, 7, 17))
                .plannedStartAt(LocalDateTime.of(2026, 7, 17, 14, 6))
                .plannedEndAt(LocalDateTime.of(2026, 7, 26, 14, 6))
                .totalWeightKg(new BigDecimal("25.50"))
                .totalVolumeM3(new BigDecimal("1.250"))
                .deliveryStopCount(1)
                .deliveryOrders(List.of(DriverDeliveryOrderResponse.builder()
                        .doId(101L)
                        .doNumber("DO-101")
                        .status(DeliveryOrderStatus.IN_TRANSIT)
                        .stopOrder(1)
                        .currentAttempt(attempt(DeliveryStatus.IN_TRANSIT))
                        .build()))
                .build();
    }

    private TripDriverViewResponse transferTripView() {
        return TripDriverViewResponse.builder()
                .tripId(901L)
                .tripNumber("TTR-20260719-0001")
                .status(TripStatus.PLANNED)
                .tripType(TripType.TRANSFER)
                .tripTypeLabel("Dieu chuyen noi bo")
                .transferId(500L)
                .driverId(401L)
                .driverName("Driver Test 2")
                .vehicleId(301L)
                .vehiclePlate("36C-88888")
                .plannedDate(LocalDate.of(2026, 7, 19))
                .plannedStartAt(LocalDateTime.of(2026, 7, 19, 8, 0))
                .plannedEndAt(LocalDateTime.of(2026, 7, 19, 17, 0))
                .totalWeightKg(new BigDecimal("40.00"))
                .totalVolumeM3(new BigDecimal("2.000"))
                .sourceWarehouseCode("HP")
                .destinationWarehouseCode("HN")
                .transferLineCount(2)
                .deliveryStopCount(0)
                .deliveryOrders(List.of())
                .build();
    }

    private DeliveryOtpResponse otp(DeliveryOtpStatus status) {
        return DeliveryOtpResponse.builder()
                .deliveryId(700L)
                .recipientEmail("dealer@example.com")
                .status(status)
                .expiresAt(OffsetDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .build();
    }

    private DeliveryAttemptResponse attempt(DeliveryStatus status) {
        return DeliveryAttemptResponse.builder()
                .deliveryId(700L)
                .attemptNumber(1)
                .status(status)
                .podImageUrl("/uploads/pod/goods.jpg")
                .podSignatureUrl("/uploads/pod/sign.jpg")
                .dispatchedAt(OffsetDateTime.now())
                .build();
    }
}
