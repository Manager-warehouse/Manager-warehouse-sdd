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
import com.wms.controller.order_fulfillment.SplitDeliveryPlanController;
import com.wms.dto.response.SplitDeliveryLegResponse;
import com.wms.dto.response.SplitLegMilestoneResponse;
import com.wms.dto.response.SplitDeliveryPlanResponse;
import com.wms.entity.access_control.User;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.order_fulfillment.SplitDeliveryPlanStatus;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.service.order_fulfillment.SplitDeliveryPlanService;
import com.wms.service.user_context.CurrentUserService;
import com.wms.util.JwtUtil;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SplitDeliveryPlanController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class SplitDeliveryPlanControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private SplitDeliveryPlanService splitDeliveryPlanService;
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
    void createPlan_returnsSplitDeliveryPlan() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(dispatcher);
        when(splitDeliveryPlanService.createPlan(any(), eq(dispatcher))).thenReturn(response(SplitDeliveryPlanStatus.PLANNED));

        mockMvc.perform(post("/api/v1/split-delivery-plans")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(splitPlanPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planNumber").value("SDP-20260802-0001"))
                .andExpect(jsonPath("$.doId").value(100))
                .andExpect(jsonPath("$.plannedStartAt").value("2026-08-03T08:00:00"))
                .andExpect(jsonPath("$.plannedEndAt").value("2026-08-03T12:00:00"))
                .andExpect(jsonPath("$.totalDriverCount").value(2))
                .andExpect(jsonPath("$.legs[0].tripId").value(501));
    }

    @Test
    @WithMockUser(username = "dispatcher@wms.com", roles = "DISPATCHER")
    void updatePlan_allowsDispatcherReplacementBeforeDeparture() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(dispatcher);
        when(splitDeliveryPlanService.updatePlan(eq(10L), any(), eq(dispatcher)))
                .thenReturn(response(SplitDeliveryPlanStatus.PLANNED));

        mockMvc.perform(put("/api/v1/split-delivery-plans/10")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(splitPlanPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLANNED"));
    }

    @Test
    @WithMockUser(username = "driver@wms.com", roles = "DRIVER")
    void confirmDriverReadiness_returnsReadinessProgress() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(driver);
        when(splitDeliveryPlanService.confirmDriverReadiness(10L, driver))
                .thenReturn(response(SplitDeliveryPlanStatus.PLANNED));

        mockMvc.perform(put("/api/v1/split-delivery-plans/10/driver-readiness").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.readyDriverCount").value(0));
    }

    @Test
    @WithMockUser(username = "driver@wms.com", roles = "DRIVER")
    void departPlan_allowsLeadDriverCoordinatedDeparture() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(driver);
        when(splitDeliveryPlanService.departPlan(10L, driver))
                .thenReturn(response(SplitDeliveryPlanStatus.IN_TRANSIT));

        mockMvc.perform(put("/api/v1/split-delivery-plans/10/depart").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.readyDriverCount").value(2));
    }

    @Test
    @WithMockUser(username = "driver@wms.com", roles = "DRIVER")
    void confirmDealerArrival_returnsSplitLegProgress() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(driver);
        when(splitDeliveryPlanService.confirmDealerArrival(10L, 11L, driver))
                .thenReturn(milestone(false, false));

        mockMvc.perform(put("/api/v1/split-delivery-plans/10/legs/11/dealer-arrival").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.splitPlanId").value(10))
                .andExpect(jsonPath("$.legId").value(11))
                .andExpect(jsonPath("$.dealerArrivedAt").exists());
    }

    @Test
    @WithMockUser(username = "driver@wms.com", roles = "DRIVER")
    void confirmHandover_returnsLeadPodOtpGate() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(driver);
        when(splitDeliveryPlanService.confirmHandover(10L, 11L, driver))
                .thenReturn(milestone(true, true));

        mockMvc.perform(put("/api/v1/split-delivery-plans/10/legs/11/handover").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allLegsHandedOver").value(true))
                .andExpect(jsonPath("$.leadPodOtpEnabled").value(true));
    }

    @Test
    @WithMockUser(username = "driver@wms.com", roles = "DRIVER")
    void failDeliveryLeg_requiresReasonAndReturnsWholePlanState() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(driver);
        when(splitDeliveryPlanService.failDeliveryLeg(eq(10L), eq(11L), any(), eq(driver)))
                .thenReturn(milestone(false, false));

        mockMvc.perform(put("/api/v1/split-delivery-plans/10/legs/11/fail-delivery")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"failureReason": "Dealer refused the full Delivery Order"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.splitPlanId").value(10));
    }

    @Test
    @WithMockUser(username = "dispatcher@wms.com", roles = "DISPATCHER")
    void cancelPlan_allowsDispatcherCancellationBeforeDeparture() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(dispatcher);
        when(splitDeliveryPlanService.cancelPlan(eq(10L), eq("Vehicle unavailable"), eq(dispatcher)))
                .thenReturn(response(SplitDeliveryPlanStatus.CANCELLED));

        mockMvc.perform(put("/api/v1/split-delivery-plans/10/cancel")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cancelReason": "Vehicle unavailable"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    private SplitDeliveryPlanResponse response(SplitDeliveryPlanStatus status) {
        return SplitDeliveryPlanResponse.builder()
                .id(10L)
                .planNumber("SDP-20260802-0001")
                .doId(100L)
                .warehouseId(20L)
                .dispatcherId(1L)
                .leadDriverId(2L)
                .status(status)
                .plannedStartAt(LocalDateTime.of(2026, 8, 3, 8, 0))
                .plannedEndAt(LocalDateTime.of(2026, 8, 3, 12, 0))
                .readyDriverCount(status == SplitDeliveryPlanStatus.IN_TRANSIT ? 2 : 0)
                .totalDriverCount(2)
                .legs(List.of(
                        leg(11L, 501L, 301L, 2L, 1, status),
                        leg(12L, 502L, 302L, 3L, 2, status)))
                .build();
    }

    private SplitLegMilestoneResponse milestone(boolean allArrived, boolean allHandedOver) {
        return SplitLegMilestoneResponse.builder()
                .splitPlanId(10L)
                .legId(11L)
                .status(SplitDeliveryPlanStatus.IN_TRANSIT)
                .dealerArrivedAt(OffsetDateTime.now())
                .handoverConfirmedAt(allHandedOver ? OffsetDateTime.now() : null)
                .allLegsArrived(allArrived)
                .allLegsHandedOver(allHandedOver)
                .leadPodOtpEnabled(allHandedOver)
                .build();
    }

    private SplitDeliveryLegResponse leg(Long id, Long tripId, Long vehicleId, Long driverId, Integer stopOrder,
            SplitDeliveryPlanStatus status) {
        return SplitDeliveryLegResponse.builder()
                .id(id)
                .tripId(tripId)
                .vehicleId(vehicleId)
                .driverId(driverId)
                .stopOrder(stopOrder)
                .status(status)
                .build();
    }

    private User user(Long id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setEmail(role.name().toLowerCase() + "@wms.com");
        user.setRole(role);
        return user;
    }

    private String splitPlanPayload() {
        return """
                {
                  "doId": 100,
                  "leadDriverId": 2,
                  "plannedStartAt": "2026-08-03T08:00:00",
                  "plannedEndAt": "2026-08-03T12:00:00",
                  "legs": [
                    {
                      "vehicleId": 301,
                      "driverId": 2,
                      "stopOrder": 1,
                      "items": [
                        {"doItemId": 401, "productId": 501, "batchId": 601, "quantity": 60}
                      ]
                    },
                    {
                      "vehicleId": 302,
                      "driverId": 3,
                      "stopOrder": 2,
                      "items": [
                        {"doItemId": 401, "productId": 501, "batchId": 601, "quantity": 40}
                      ]
                    }
                  ]
                }
                """;
    }
}
