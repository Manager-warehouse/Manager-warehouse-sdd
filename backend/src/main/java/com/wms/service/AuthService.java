package com.wms.service;

import com.wms.dto.auth.*;
import com.wms.entity.User;
import com.wms.entity.UserWarehouseAssignment;
import com.wms.entity.AuditLog;
import com.wms.enums.AuditAction;
import com.wms.repository.UserRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.AuditLogRepository;
import com.wms.util.JwtUtil;
import com.wms.util.AuditLogUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JavaMailSender mailSender;
    private final UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;
    private final AuditLogRepository auditLogRepository;

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (AuthenticationException e) {
            throw new IllegalArgumentException("INVALID_CREDENTIALS");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("INVALID_CREDENTIALS"));

        if (!user.getIsActive()) {
            throw new IllegalStateException("ACCOUNT_INACTIVE");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole().name());
        String rawRefreshToken = UUID.randomUUID().toString();

        user.setRefreshTokenHash(sha256(rawRefreshToken));
        user.setRefreshTokenExpiresAt(OffsetDateTime.now().plusSeconds(refreshTokenExpiry));
        userRepository.save(user);

        List<LoginResponse.WarehouseInfo> warehouses = buildWarehouseInfoList(user);
        List<Long> warehouseIds = userWarehouseAssignmentRepository.findWarehouseIdsByUserId(user.getId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .warehouses(warehouseIds)
                        .assignedWarehouses(warehouses)
                        .build())
                .build();
    }

    @Transactional
    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        String tokenHash = sha256(request.getRefreshToken());

        User user = userRepository.findAll().stream()
                .filter(u -> tokenHash.equals(u.getRefreshTokenHash()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("TOKEN_INVALID"));

        if (user.getRefreshTokenExpiresAt() == null ||
                user.getRefreshTokenExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("TOKEN_EXPIRED");
        }

        String newAccessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole().name());

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .build();
    }

    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        user.setRefreshTokenHash(null);
        user.setRefreshTokenExpiresAt(null);
        userRepository.save(user);
    }

    public MeResponse me(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        List<MeResponse.WarehouseInfo> warehouses = buildMeWarehouseInfoList(user);
        List<Long> warehouseIds = userWarehouseAssignmentRepository.findWarehouseIdsByUserId(user.getId());

        return MeResponse.builder()
                .id(user.getId())
                .code(user.getCode())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .jobTitle(user.getJobTitle())
                .shift(user.getShift())
                .region(user.getRegion())
                .isActive(user.getIsActive())
                .warehouses(warehouseIds)
                .assignedWarehouses(warehouses)
                .build();
    }

    @Transactional
    public MeResponse updateProfile(String currentEmail, ProfileUpdateRequest request) {
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        // Check email uniqueness if email is changing
        if (!user.getEmail().equalsIgnoreCase(request.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new IllegalArgumentException("EMAIL_TAKEN");
            }
        }

        // Capture old values for audit logging
        java.util.Map<String, Object> oldValue = java.util.Map.of(
                "fullName", user.getFullName(),
                "email", user.getEmail(),
                "phone", user.getPhone() != null ? user.getPhone() : ""
        );

        // Perform update
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setUpdatedAt(OffsetDateTime.now());

        User savedUser = userRepository.save(user);

        // Capture new values
        java.util.Map<String, Object> newValue = java.util.Map.of(
                "fullName", savedUser.getFullName(),
                "email", savedUser.getEmail(),
                "phone", savedUser.getPhone() != null ? savedUser.getPhone() : ""
        );

        // Save audit log
        AuditLog auditLog = AuditLog.builder()
                .actor(savedUser)
                .actorRole(savedUser.getRole().name())
                .action(AuditAction.UPDATE)
                .entityType("User")
                .entityId(savedUser.getId())
                .description("UPDATE User Profile: " + savedUser.getEmail())
                .oldValue(AuditLogUtil.toJson(oldValue))
                .newValue(AuditLogUtil.toJson(newValue))
                .timestamp(OffsetDateTime.now())
                .build();
        auditLogRepository.save(auditLog);

        return me(savedUser.getEmail());
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String otp = String.format("%06d", new Random().nextInt(1_000_000));
            user.setOtpHash(sha256(otp));
            user.setOtpExpiresAt(OffsetDateTime.now().plusMinutes(10));
            userRepository.save(user);
            sendOtpEmail(user.getEmail(), otp);
        });
        // Always return silently — no email enumeration
    }

    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("OTP_INVALID"));

        if (user.getOtpHash() == null || !user.getOtpHash().equals(sha256(request.getOtp()))) {
            throw new IllegalArgumentException("OTP_INVALID");
        }

        if (user.getOtpExpiresAt() == null || user.getOtpExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("OTP_EXPIRED");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setOtpHash(null);
        user.setOtpExpiresAt(null);
        // Invalidate any active sessions after password reset
        user.setRefreshTokenHash(null);
        user.setRefreshTokenExpiresAt(null);
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("INVALID_CREDENTIALS");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // --- helpers ---

    private void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Mã OTP đặt lại mật khẩu — WMS Phúc Anh");
        message.setText("Mã OTP của bạn là: " + otp + "\nMã có hiệu lực trong 10 phút. Không chia sẻ mã này với bất kỳ ai.");
        mailSender.send(message);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private List<LoginResponse.WarehouseInfo> buildWarehouseInfoList(User user) {
        // UserWarehouseAssignment is loaded via separate query to avoid N+1
        // For now returning empty list — will be populated when UserWarehouseAssignmentRepository is added
        return List.of();
    }

    private List<MeResponse.WarehouseInfo> buildMeWarehouseInfoList(User user) {
        return List.of();
    }
}
