package com.wms.controller.access_control;


import com.wms.dto.auth.ChangePasswordRequest;
import com.wms.dto.auth.CheckOtpRequest;
import com.wms.dto.auth.ForgotPasswordRequest;
import com.wms.dto.auth.LoginRequest;
import com.wms.dto.auth.LoginResponse;
import com.wms.dto.auth.MeResponse;
import com.wms.dto.auth.ProfileUpdateRequest;
import com.wms.dto.auth.RefreshTokenRequest;
import com.wms.dto.auth.RefreshTokenResponse;
import com.wms.dto.auth.VerifyOtpRequest;
import com.wms.service.access_control.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller xác thực người dùng (Spec 001).
 * Cung cấp các endpoint: đăng nhập, làm mới token, đăng xuất, quên/đổi mật khẩu, cập nhật hồ sơ.
 * Tất cả endpoint /login, /refresh, /forgot-password, /otp/check, /verify-otp là public (không cần JWT).
 * Các endpoint còn lại yêu cầu JWT hợp lệ.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Đăng nhập, làm mới token, đăng xuất, quên/đổi mật khẩu")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Đăng nhập")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Làm mới access token")
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @Operation(summary = "Đăng xuất")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) RefreshTokenRequest request) {
        authService.logout(userDetails.getUsername(), request != null ? request.getRefreshToken() : null);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Thông tin người dùng hiện tại")
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authService.me(userDetails.getUsername()));
    }

    @Operation(summary = "Quên mật khẩu — gửi OTP qua email")
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message", "OTP đã được gửi đến email của bạn."));
    }

    @Operation(summary = "Xác minh OTP và đặt mật khẩu mới")
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request);
        return ResponseEntity.ok(Map.of("message", "Mật khẩu đã được đặt lại thành công."));
    }

    @Operation(summary = "Kiểm tra mã OTP hợp lệ (không tiêu thụ, dùng cho bước xác nhận OTP)")
    @PostMapping("/otp/check")
    public ResponseEntity<Map<String, String>> checkOtp(@Valid @RequestBody CheckOtpRequest request) {
        authService.checkOtp(request);
        return ResponseEntity.ok(Map.of("message", "Mã OTP hợp lệ."));
    }

    @Operation(summary = "Đổi mật khẩu")
    @PutMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cập nhật hồ sơ cá nhân")
    @PutMapping("/profile")
    public ResponseEntity<MeResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(authService.updateProfile(userDetails.getUsername(), request));
    }
}
