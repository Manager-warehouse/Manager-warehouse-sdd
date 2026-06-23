package com.wms.dto.response;

import com.wms.enums.TripStatus;
import com.wms.enums.TripType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TripResponse {
    private Long id;
    private String tripNumber;
    private Long warehouseId;
    private Long vehicleId;
    private Long driverId;
    private Long dispatcherId;
    private LocalDateTime plannedStartAt;
    private LocalDateTime plannedEndAt;
    private TripType tripType;
    private TripStatus status;
    private BigDecimal totalWeightKg;
    private BigDecimal totalVolumeM3;
    private String cancelReason;
    private OffsetDateTime departedAt;
    private OffsetDateTime completedAt;
    private String notes;
    private List<TripDeliveryOrderResponse> deliveryOrders;
}
