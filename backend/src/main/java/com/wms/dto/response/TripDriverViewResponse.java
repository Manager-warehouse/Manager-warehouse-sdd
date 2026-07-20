package com.wms.dto.response;

import com.wms.enums.TripStatus;
import com.wms.enums.TripType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TripDriverViewResponse {
    private Long tripId;
    private String tripNumber;
    private TripStatus status;
    private TripType tripType;
    private String tripTypeLabel;
    private Long transferId;
    private Long driverId;
    private String driverName;
    private Long vehicleId;
    private String vehiclePlate;
    private LocalDate plannedDate;
    private LocalDateTime plannedStartAt;
    private LocalDateTime plannedEndAt;
    private BigDecimal totalWeightKg;
    private BigDecimal totalVolumeM3;
    private Integer deliveryStopCount;
    private String sourceWarehouseCode;
    private String destinationWarehouseCode;
    private Integer transferLineCount;
    private List<DriverDeliveryOrderResponse> deliveryOrders;
}
