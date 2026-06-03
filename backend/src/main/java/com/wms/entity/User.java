package com.wms.entity;

import com.wms.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private UserRole role;

    @Column(name = "job_title", length = 100)
    private String jobTitle;

    @Column(name = "shift", length = 50)
    private String shift;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Auth: refresh token (stored as SHA-256 hash)
    @Column(name = "refresh_token_hash", length = 255)
    private String refreshTokenHash;

    @Column(name = "refresh_token_expires_at")
    private OffsetDateTime refreshTokenExpiresAt;

    // Auth: OTP for password reset (stored as SHA-256 hash)
    @Column(name = "otp_hash", length = 255)
    private String otpHash;

    @Column(name = "otp_expires_at")
    private OffsetDateTime otpExpiresAt;
}
