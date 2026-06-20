package com.wms.dto.response;

import com.wms.enums.DeliveryOrderStatus;
import com.wms.enums.DeliveryOrderType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DeliveryOrderResponse {
    private Long id;
    private String doNumber;
    private Long dealerId;
    private Long warehouseId;
    private DeliveryOrderType type;
    private LocalDate expectedDeliveryDate;
    private DeliveryOrderStatus status;
    private String cancelReason;
    private String rejectionReason;
    private BigDecimal totalPickedQty;
    private BigDecimal totalQcPassQty;
    private BigDecimal totalQcFailQty;
    private LocalDate documentDate;
    private String notes;
    private List<DeliveryOrderItemResponse> items;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
