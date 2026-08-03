package com.wms.dto.auth;


import lombok.Builder;
import lombok.Getter;

/** DTO phản hồi làm mới token — chứa access token và refresh token mới (Spec 001). */
@Getter
@Builder
public class RefreshTokenResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
}
