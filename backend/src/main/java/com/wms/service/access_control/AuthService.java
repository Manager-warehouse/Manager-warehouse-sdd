package com.wms.service.access_control;


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
import com.wms.dto.auth.*;
import com.wms.entity.access_control.User;
import com.wms.entity.audit_trail.AuditLog;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.repository.UserRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.AuditLogRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.enums.access_control.UserRole;
import com.wms.util.JwtUtil;
import com.wms.util.AuditLogUtil;
import com.wms.service.notification_delivery.EmailService;
import lombok.RequiredArgsConstructor;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;
    private final AuditLogRepository auditLogRepository;
    private final WarehouseRepository warehouseRepository;

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

        User user = userRepository.findByRefreshTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("TOKEN_INVALID"));

        if (user.getRefreshTokenExpiresAt() == null ||
                user.getRefreshTokenExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("TOKEN_EXPIRED");
        }

        String newAccessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole().name());

        // Rotate the refresh token on every use so a stolen token cannot be
        // replayed indefinitely — the old hash stops working immediately.
        String newRawRefreshToken = UUID.randomUUID().toString();
        user.setRefreshTokenHash(sha256(newRawRefreshToken));
        user.setRefreshTokenExpiresAt(OffsetDateTime.now().plusSeconds(refreshTokenExpiry));
        userRepository.save(user);

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRawRefreshToken)
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
                "phone", user.getPhone() != null ? user.getPhone() : "");

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
                "phone", savedUser.getPhone() != null ? savedUser.getPhone() : "");

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

    private static final int MAX_OTP_ATTEMPTS = 5;

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
            user.setOtpHash(sha256(otp));
            user.setOtpExpiresAt(OffsetDateTime.now().plusMinutes(10));
            user.setOtpAttemptCount(0);
            userRepository.saveAndFlush(user);
            emailService.sendOtpEmail(user.getEmail(), otp);
        });
        // Always return silently — no email enumeration
    }

    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("OTP_INVALID"));

        if (user.getOtpExpiresAt() == null || user.getOtpExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("OTP_EXPIRED");
        }

        if (user.getOtpAttemptCount() != null && user.getOtpAttemptCount() >= MAX_OTP_ATTEMPTS) {
            throw new IllegalArgumentException("OTP_LOCKED");
        }

        if (user.getOtpHash() == null || !user.getOtpHash().equals(sha256(request.getOtp()))) {
            user.setOtpAttemptCount((user.getOtpAttemptCount() == null ? 0 : user.getOtpAttemptCount()) + 1);
            userRepository.save(user);
            throw new IllegalArgumentException("OTP_INVALID");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setOtpHash(null);
        user.setOtpExpiresAt(null);
        user.setOtpAttemptCount(0);
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

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("NEW_PASSWORD_SAME_AS_CURRENT");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        // Invalidate any active sessions — if the current one was compromised,
        // the attacker's refresh token must stop working the moment the
        // legitimate owner changes their password.
        user.setRefreshTokenHash(null);
        user.setRefreshTokenExpiresAt(null);
        userRepository.save(user);
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
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.CEO) {
            return warehouseRepository.findAll().stream()
                    .filter(Warehouse::getIsActive)
                    .map(w -> LoginResponse.WarehouseInfo.builder()
                            .id(w.getId())
                            .name(w.getName())
                            .build())
                    .collect(Collectors.toList());
        } else {
            List<Long> assignedIds = userWarehouseAssignmentRepository.findWarehouseIdsByUserId(user.getId());
            return warehouseRepository.findAllById(assignedIds).stream()
                    .map(w -> LoginResponse.WarehouseInfo.builder()
                            .id(w.getId())
                            .name(w.getName())
                            .build())
                    .collect(Collectors.toList());
        }
    }

    private List<MeResponse.WarehouseInfo> buildMeWarehouseInfoList(User user) {
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.CEO) {
            return warehouseRepository.findAll().stream()
                    .filter(Warehouse::getIsActive)
                    .map(w -> MeResponse.WarehouseInfo.builder()
                            .id(w.getId())
                            .name(w.getName())
                            .build())
                    .collect(Collectors.toList());
        } else {
            List<Long> assignedIds = userWarehouseAssignmentRepository.findWarehouseIdsByUserId(user.getId());
            return warehouseRepository.findAllById(assignedIds).stream()
                    .map(w -> MeResponse.WarehouseInfo.builder()
                            .id(w.getId())
                            .name(w.getName())
                            .build())
                    .collect(Collectors.toList());
        }
    }

}
