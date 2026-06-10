package com.wms.dto.auth;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private UserInfo user;

    @Getter
    @Builder
    public static class UserInfo {
        private Long id;
        private String fullName;
        private String email;
        private String role;
        private List<Long> warehouses;
        private List<WarehouseInfo> assignedWarehouses;
    }

    @Getter
    @Builder
    public static class WarehouseInfo {
        private Long id;
        private String name;
    }
}
