package com.wms.controller;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.controller.order_fulfillment.PodEvidenceController;
import com.wms.entity.access_control.User;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.service.order_fulfillment.DriverDeliveryService;
import com.wms.service.order_fulfillment.PodEvidenceStorageService.StoredPodContent;
import com.wms.service.user_context.CurrentUserService;
import com.wms.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PodEvidenceController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class PodEvidenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DriverDeliveryService driverDeliveryService;
    @MockBean
    private CurrentUserService currentUserService;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    private User viewer;

    @BeforeEach
    void setUp() {
        viewer = new User();
        viewer.setId(2L);
        viewer.setRole(UserRole.PLANNER);
    }

    @Test
    @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
    void getPodEvidenceImage_streamsInlineImage() throws Exception {
        byte[] bytes = new byte[] { (byte) 0xff, (byte) 0xd8, (byte) 0xff };
        when(currentUserService.getRequiredCurrentUser()).thenReturn(viewer);
        when(driverDeliveryService.getPodEvidence(101L, "GOODS", viewer))
                .thenReturn(new StoredPodContent(bytes, "goods.jpg", "image/jpeg"));

        mockMvc.perform(get("/api/v1/delivery-orders/101/pod-evidence/GOODS"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"goods.jpg\""))
                .andExpect(content().bytes(bytes));
    }

    @Test
    @WithMockUser(username = "driver@wms.com", roles = "DRIVER")
    void getPodEvidenceImage_rejectsRoleWithoutOrderDetailAccess() throws Exception {
        mockMvc.perform(get("/api/v1/delivery-orders/101/pod-evidence/GOODS"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(driverDeliveryService);
    }
}
