package com.wms.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class WarehouseResponse {
    private Long id;
    private String code;
    private String name;
    private String address;
    private String phone;
    private Long managerId;
    private String managerName;
    private String type;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
