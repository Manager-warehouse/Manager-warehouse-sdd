package com.wms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.dto.auth.*;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.service.AuthService;
import com.wms.util.JwtUtil;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService authService;
    @MockBean JwtUtil jwtUtil;
    @MockBean UserDetailsServiceImpl userDetailsService;

    // ─── POST /api/v1/auth/login ──────────────────────────────────────────────

    @Test
    @DisplayName("POST /login — 200 OK với credentials đúng")
    void login_validCredentials_returns200() throws Exception {
        LoginResponse response = LoginResponse.builder()
                .accessToken("access-token-abc")
                .refreshToken("refresh-token-xyz")
                .tokenType("Bearer")
                .expiresIn(900)
                .user(LoginResponse.UserInfo.builder()
                        .id(1L)
                        .fullName("Nguyen Van A")
                        .email("test@wms.com")
                        .role("STOREKEEPER")
                        .assignedWarehouses(List.of())
                        .build())
                .build();

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"test@wms.com","password":"Password@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-abc"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-xyz"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.role").value("STOREKEEPER"));
    }

    @Test
    @DisplayName("POST /login — 401 khi credentials sai")
    void login_invalidCredentials_returns401() throws Exception {
        when(authService.login(any()))
                .thenThrow(new IllegalArgumentException("INVALID_CREDENTIALS"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"test@wms.com","password":"wrongpass"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("POST /login — 401 khi tài khoản bị vô hiệu hóa")
    void login_inactiveAccount_returns401() throws Exception {
        when(authService.login(any()))
                .thenThrow(new IllegalStateException("ACCOUNT_INACTIVE"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"inactive@wms.com","password":"Password@123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ACCOUNT_INACTIVE"));
    }

    @Test
    @DisplayName("POST /login — 400 khi thiếu email hoặc password")
    void login_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"","password":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /login — 400 khi email không đúng định dạng")
    void login_invalidEmailFormat_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"Password@123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ─── POST /api/v1/auth/refresh ────────────────────────────────────────────

    @Test
    @DisplayName("POST /refresh — 200 OK với refresh token hợp lệ")
    void refresh_validToken_returns200() throws Exception {
        RefreshTokenResponse response = RefreshTokenResponse.builder()
                .accessToken("new-access-token")
                .tokenType("Bearer")
                .expiresIn(900)
                .build();

        when(authService.refresh(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"valid-refresh-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    @Test
    @DisplayName("POST /refresh — 401 khi refresh token không hợp lệ")
    void refresh_invalidToken_returns401() throws Exception {
        when(authService.refresh(any()))
                .thenThrow(new IllegalArgumentException("TOKEN_INVALID"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"bad-token"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("TOKEN_INVALID"));
    }

    @Test
    @DisplayName("POST /refresh — 401 khi refresh token đã hết hạn")
    void refresh_expiredToken_returns401() throws Exception {
        when(authService.refresh(any()))
                .thenThrow(new IllegalArgumentException("TOKEN_EXPIRED"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"expired-token"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("TOKEN_EXPIRED"));
    }

    // ─── POST /api/v1/auth/logout ─────────────────────────────────────────────

    @Test
    @DisplayName("POST /logout — 204 No Content khi đăng xuất thành công")
    @WithMockUser(username = "test@wms.com")
    void logout_authenticatedUser_returns204() throws Exception {
        doNothing().when(authService).logout("test@wms.com");

        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent());

        verify(authService).logout("test@wms.com");
    }

    @Test
    @DisplayName("POST /logout — 401 khi không có token")
    void logout_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    // ─── GET /api/v1/auth/me ──────────────────────────────────────────────────

    @Test
    @DisplayName("GET /me — 200 OK trả về thông tin user đang đăng nhập")
    @WithMockUser(username = "test@wms.com")
    void me_authenticatedUser_returns200() throws Exception {
        MeResponse response = MeResponse.builder()
                .id(1L)
                .code("U001")
                .fullName("Nguyen Van A")
                .email("test@wms.com")
                .role("STOREKEEPER")
                .assignedWarehouses(List.of())
                .build();

        when(authService.me("test@wms.com")).thenReturn(response);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@wms.com"))
                .andExpect(jsonPath("$.fullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.role").value("STOREKEEPER"));
    }

    @Test
    @DisplayName("GET /me — 401 khi không có token")
    void me_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    // ─── POST /api/v1/auth/forgot-password ────────────────────────────────────

    @Test
    @DisplayName("POST /forgot-password — 200 OK dù email không tồn tại (chống enumeration)")
    void forgotPassword_anyEmail_returns200() throws Exception {
        doNothing().when(authService).forgotPassword(any());

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"anyone@wms.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP đã được gửi đến email của bạn."));
    }

    @Test
    @DisplayName("POST /forgot-password — 400 khi thiếu email")
    void forgotPassword_missingEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ─── POST /api/v1/auth/verify-otp ─────────────────────────────────────────

    @Test
    @DisplayName("POST /verify-otp — 200 OK khi OTP đúng")
    void verifyOtp_validOtp_returns200() throws Exception {
        doNothing().when(authService).verifyOtp(any());

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"test@wms.com","otp":"123456","newPassword":"NewPass@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Mật khẩu đã được đặt lại thành công."));
    }

    @Test
    @DisplayName("POST /verify-otp — 400 khi OTP sai")
    void verifyOtp_wrongOtp_returns400() throws Exception {
        doThrow(new IllegalArgumentException("OTP_INVALID")).when(authService).verifyOtp(any());

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"test@wms.com","otp":"999999","newPassword":"NewPass@123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("OTP_INVALID"));
    }

    @Test
    @DisplayName("POST /verify-otp — 400 khi OTP hết hạn")
    void verifyOtp_expiredOtp_returns400() throws Exception {
        doThrow(new IllegalArgumentException("OTP_EXPIRED")).when(authService).verifyOtp(any());

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"test@wms.com","otp":"123456","newPassword":"NewPass@123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("OTP_EXPIRED"));
    }

    // ─── PUT /api/v1/auth/change-password ─────────────────────────────────────

    @Test
    @DisplayName("PUT /change-password — 204 khi đổi mật khẩu thành công")
    @WithMockUser(username = "test@wms.com")
    void changePassword_validRequest_returns204() throws Exception {
        doNothing().when(authService).changePassword(anyString(), any());

        mockMvc.perform(put("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"OldPass@123","newPassword":"NewPass@456"}
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PUT /change-password — 401 khi mật khẩu hiện tại sai")
    @WithMockUser(username = "test@wms.com")
    void changePassword_wrongCurrentPassword_returns401() throws Exception {
        doThrow(new IllegalArgumentException("INVALID_CREDENTIALS"))
                .when(authService).changePassword(anyString(), any());

        mockMvc.perform(put("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"WrongPass","newPassword":"NewPass@456"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("PUT /change-password — 401 khi không có token")
    void changePassword_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"OldPass@123","newPassword":"NewPass@456"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ─── PUT /api/v1/auth/profile ─────────────────────────────────────────────

    @Test
    @DisplayName("PUT /profile — 200 OK khi cập nhật thành công")
    @WithMockUser(username = "test@wms.com")
    void updateProfile_validRequest_returns200() throws Exception {
        MeResponse response = MeResponse.builder()
                .id(1L)
                .code("U001")
                .fullName("Nguyen Van B")
                .email("newemail@wms.com")
                .phone("0987654321")
                .role("STOREKEEPER")
                .assignedWarehouses(List.of())
                .build();

        when(authService.updateProfile(eq("test@wms.com"), any(ProfileUpdateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Nguyen Van B","email":"newemail@wms.com","phone":"0987654321"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Nguyen Van B"))
                .andExpect(jsonPath("$.email").value("newemail@wms.com"))
                .andExpect(jsonPath("$.phone").value("0987654321"));
    }

    @Test
    @DisplayName("PUT /profile — 400 Bad Request khi thiếu fullName")
    @WithMockUser(username = "test@wms.com")
    void updateProfile_missingFullName_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"","email":"newemail@wms.com","phone":"0987654321"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /profile — 401 khi không có token")
    void updateProfile_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Nguyen Van B","email":"newemail@wms.com","phone":"0987654321"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}

