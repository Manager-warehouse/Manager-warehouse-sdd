package com.wms.service;

import com.wms.dto.auth.*;
import com.wms.entity.User;
import com.wms.enums.UserRole;
import com.wms.repository.UserRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.AuditLogRepository;
import com.wms.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JavaMailSender mailSender;
    @Mock private UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;
    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuthService authService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiry", 604800L);

        activeUser = User.builder()
                .id(1L)
                .code("U001")
                .fullName("Nguyen Van A")
                .email("test@wms.com")
                .passwordHash("$2a$12$hashedpassword")
                .role(UserRole.STOREKEEPER)
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    // ─── LOGIN ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Login thành công với email và password đúng")
    void login_success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@wms.com");
        req.setPassword("password123");

        when(userRepository.findByEmail("test@wms.com")).thenReturn(Optional.of(activeUser));
        when(jwtUtil.generateAccessToken(anyString(), anyString())).thenReturn("access-token-123");
        when(userRepository.save(any(User.class))).thenReturn(activeUser);

        LoginResponse response = authService.login(req);

        assertThat(response.getAccessToken()).isEqualTo("access-token-123");
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(900);
        assertThat(response.getUser().getEmail()).isEqualTo("test@wms.com");
        assertThat(response.getUser().getRole()).isEqualTo("STOREKEEPER");
        verify(userRepository).save(activeUser);
    }

    @Test
    @DisplayName("Login thất bại khi sai password")
    void login_wrongPassword_throwsInvalidCredentials() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@wms.com");
        req.setPassword("wrongpassword");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("Login thất bại khi tài khoản bị vô hiệu hóa (is_active = false)")
    void login_inactiveAccount_throwsAccountInactive() {
        activeUser.setIsActive(false);

        LoginRequest req = new LoginRequest();
        req.setEmail("test@wms.com");
        req.setPassword("password123");

        when(userRepository.findByEmail("test@wms.com")).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ACCOUNT_INACTIVE");
    }

    // ─── REFRESH TOKEN ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Refresh token hợp lệ trả về access token mới")
    void refresh_validToken_returnsNewAccessToken() {
        String rawToken = "valid-refresh-token";
        activeUser.setRefreshTokenHash(sha256(rawToken));
        activeUser.setRefreshTokenExpiresAt(OffsetDateTime.now().plusDays(7));

        when(userRepository.findByRefreshTokenHash(sha256(rawToken))).thenReturn(Optional.of(activeUser));
        when(jwtUtil.generateAccessToken(anyString(), anyString())).thenReturn("new-access-token");

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken(rawToken);

        RefreshTokenResponse response = authService.refresh(req);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("Refresh thất bại khi token không tồn tại")
    void refresh_invalidToken_throwsTokenInvalid() {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("nonexistent-token");

        assertThatThrownBy(() -> authService.refresh(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("TOKEN_INVALID");
    }

    @Test
    @DisplayName("Refresh thất bại khi token đã hết hạn")
    void refresh_expiredToken_throwsTokenExpired() {
        String rawToken = "expired-refresh-token";
        activeUser.setRefreshTokenHash(sha256(rawToken));
        activeUser.setRefreshTokenExpiresAt(OffsetDateTime.now().minusDays(1));

        when(userRepository.findByRefreshTokenHash(sha256(rawToken))).thenReturn(Optional.of(activeUser));

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken(rawToken);

        assertThatThrownBy(() -> authService.refresh(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("TOKEN_EXPIRED");
    }

    // ─── LOGOUT ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Logout xóa refresh token khỏi DB")
    void logout_clearsRefreshToken() {
        activeUser.setRefreshTokenHash("some-hash");
        activeUser.setRefreshTokenExpiresAt(OffsetDateTime.now().plusDays(7));

        when(userRepository.findByEmail("test@wms.com")).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any())).thenReturn(activeUser);

        authService.logout("test@wms.com");

        assertThat(activeUser.getRefreshTokenHash()).isNull();
        assertThat(activeUser.getRefreshTokenExpiresAt()).isNull();
        verify(userRepository).save(activeUser);
    }

    // ─── ME ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Me trả về thông tin đúng của user đang đăng nhập")
    void me_returnsUserInfo() {
        when(userRepository.findByEmail("test@wms.com")).thenReturn(Optional.of(activeUser));

        MeResponse response = authService.me("test@wms.com");

        assertThat(response.getEmail()).isEqualTo("test@wms.com");
        assertThat(response.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(response.getRole()).isEqualTo("STOREKEEPER");
        assertThat(response.getCode()).isEqualTo("U001");
    }

    // ─── FORGOT PASSWORD ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Quên mật khẩu gửi OTP khi email tồn tại")
    void forgotPassword_validEmail_sendsOtp() {
        when(userRepository.findByEmail("test@wms.com")).thenReturn(Optional.of(activeUser));
        when(userRepository.saveAndFlush(any())).thenReturn(activeUser);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("test@wms.com");

        assertThatCode(() -> authService.forgotPassword(req)).doesNotThrowAnyException();

        assertThat(activeUser.getOtpHash()).isNotNull();
        assertThat(activeUser.getOtpExpiresAt()).isAfter(OffsetDateTime.now());
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Quên mật khẩu không báo lỗi khi email không tồn tại (chống email enumeration)")
    void forgotPassword_unknownEmail_silentNoError() {
        when(userRepository.findByEmail("unknown@wms.com")).thenReturn(Optional.empty());

        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("unknown@wms.com");

        assertThatCode(() -> authService.forgotPassword(req)).doesNotThrowAnyException();
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    // ─── VERIFY OTP ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Verify OTP đúng đặt lại mật khẩu thành công")
    void verifyOtp_validOtp_resetsPassword() {
        String otp = "123456";
        activeUser.setOtpHash(sha256(otp));
        activeUser.setOtpExpiresAt(OffsetDateTime.now().plusMinutes(5));

        when(userRepository.findByEmail("test@wms.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.encode("NewPass@123")).thenReturn("hashed-new-pass");
        when(userRepository.save(any())).thenReturn(activeUser);

        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail("test@wms.com");
        req.setOtp(otp);
        req.setNewPassword("NewPass@123");

        assertThatCode(() -> authService.verifyOtp(req)).doesNotThrowAnyException();

        assertThat(activeUser.getPasswordHash()).isEqualTo("hashed-new-pass");
        assertThat(activeUser.getOtpHash()).isNull();
        assertThat(activeUser.getRefreshTokenHash()).isNull();
    }

    @Test
    @DisplayName("Verify OTP thất bại khi OTP sai")
    void verifyOtp_wrongOtp_throwsOtpInvalid() {
        activeUser.setOtpHash(sha256("123456"));
        activeUser.setOtpExpiresAt(OffsetDateTime.now().plusMinutes(5));

        when(userRepository.findByEmail("test@wms.com")).thenReturn(Optional.of(activeUser));

        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail("test@wms.com");
        req.setOtp("999999");
        req.setNewPassword("NewPass@123");

        assertThatThrownBy(() -> authService.verifyOtp(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OTP_INVALID");
    }

    @Test
    @DisplayName("Verify OTP thất bại khi OTP đã hết hạn")
    void verifyOtp_expiredOtp_throwsOtpExpired() {
        String otp = "123456";
        activeUser.setOtpHash(sha256(otp));
        activeUser.setOtpExpiresAt(OffsetDateTime.now().minusMinutes(1));

        when(userRepository.findByEmail("test@wms.com")).thenReturn(Optional.of(activeUser));

        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail("test@wms.com");
        req.setOtp(otp);
        req.setNewPassword("NewPass@123");

        assertThatThrownBy(() -> authService.verifyOtp(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OTP_EXPIRED");
    }

    // ─── CHANGE PASSWORD ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Đổi mật khẩu thành công với mật khẩu hiện tại đúng")
    void changePassword_correctCurrentPassword_success() {
        when(userRepository.findByEmail("test@wms.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("OldPass@123", activeUser.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("NewPass@456")).thenReturn("hashed-new-pass");
        when(userRepository.save(any())).thenReturn(activeUser);

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("OldPass@123");
        req.setNewPassword("NewPass@456");

        assertThatCode(() -> authService.changePassword("test@wms.com", req)).doesNotThrowAnyException();
        assertThat(activeUser.getPasswordHash()).isEqualTo("hashed-new-pass");
    }

    @Test
    @DisplayName("Đổi mật khẩu thất bại khi mật khẩu hiện tại sai")
    void changePassword_wrongCurrentPassword_throwsInvalidCredentials() {
        when(userRepository.findByEmail("test@wms.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("WrongPass", activeUser.getPasswordHash())).thenReturn(false);

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("WrongPass");
        req.setNewPassword("NewPass@456");

        assertThatThrownBy(() -> authService.changePassword("test@wms.com", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INVALID_CREDENTIALS");
    }

    // ─── UPDATE PROFILE ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Cập nhật profile thành công khi email không trùng")
    void updateProfile_success() {
        when(userRepository.findByEmail("test@wms.com")).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenReturn(activeUser);

        ProfileUpdateRequest req = new ProfileUpdateRequest();
        req.setFullName("Nguyen Van B");
        req.setEmail("newemail@wms.com");
        req.setPhone("0987654321");

        when(userRepository.findByEmail("newemail@wms.com"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(activeUser));

        MeResponse response = authService.updateProfile("test@wms.com", req);

        assertThat(response.getFullName()).isEqualTo("Nguyen Van B");
        assertThat(response.getEmail()).isEqualTo("newemail@wms.com");
        assertThat(response.getPhone()).isEqualTo("0987654321");
        verify(userRepository).save(activeUser);
        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("Cập nhật profile thất bại khi email mới đã bị người khác dùng")
    void updateProfile_emailTaken_throwsException() {
        when(userRepository.findByEmail("test@wms.com")).thenReturn(Optional.of(activeUser));

        ProfileUpdateRequest req = new ProfileUpdateRequest();
        req.setFullName("Nguyen Van B");
        req.setEmail("other@wms.com");
        req.setPhone("0987654321");

        User otherUser = User.builder().id(2L).email("other@wms.com").build();
        when(userRepository.findByEmail("other@wms.com")).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> authService.updateProfile("test@wms.com", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("EMAIL_TAKEN");
        verify(userRepository, never()).save(any());
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private String sha256(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
