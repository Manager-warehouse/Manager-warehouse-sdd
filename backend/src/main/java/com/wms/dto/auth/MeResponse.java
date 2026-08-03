package com.wms.dto.auth;


import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** DTO phản hồi thông tin user hiện tại — GET /me (Spec 001). */
@Getter
@Builder
public class MeResponse {

    private Long id;
    private String code;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String jobTitle;
    private String shift;
    private String region;
    private Boolean isActive;
    private List<Long> warehouses;
    private List<WarehouseInfo> assignedWarehouses;

    @Getter
    @Builder
    public static class WarehouseInfo {
        private Long id;
        private String name;
    }
}
