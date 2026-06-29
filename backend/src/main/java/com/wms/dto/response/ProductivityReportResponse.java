package com.wms.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductivityReportResponse {
    private Long warehouseId;
    private String warehouseName;
    private String startDate;
    private String endDate;
    private List<StaffProductivity> staffProductivity;
    private List<StorekeeperProductivity> storekeeperProductivity;
    private List<DriverProductivity> driverProductivity;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StaffProductivity {
        private String employeeCode;
        private String fullName;
        private String role;
        private Integer pickingRunsCount;
        private BigDecimal totalPickedQty;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorekeeperProductivity {
        private String employeeCode;
        private String fullName;
        private String role;
        private Integer pickingPlansCreated;
        private BigDecimal totalQcCheckedQty;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DriverProductivity {
        private String employeeCode;
        private String fullName;
        private String role;
        private Integer tripsCompleted;
        private Integer successfulDeliveries;
    }
}
