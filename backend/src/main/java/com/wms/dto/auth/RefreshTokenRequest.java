package com.wms.dto.auth;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** DTO yêu cầu làm mới token — chứa refresh token (Spec 001). */
@Getter
@Setter
public class RefreshTokenRequest {

    @NotBlank
    private String refreshToken;
}
