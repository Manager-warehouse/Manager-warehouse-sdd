package com.wms.dto.auth;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

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
    private List<WarehouseInfo> assignedWarehouses;

    @Getter
    @Builder
    public static class WarehouseInfo {
        private Long id;
        private String name;
    }
}
