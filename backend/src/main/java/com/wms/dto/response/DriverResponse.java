package com.wms.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
public class DriverResponse {
    private Long id;
    private Long userId;
    private List<Long> warehouseIds;
    private String fullName;
    private String phone;
    private String licenseNumber;
    private LocalDate licenseExpiry;
    private String status;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
