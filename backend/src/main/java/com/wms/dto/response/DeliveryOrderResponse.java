package com.wms.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.wms.enums.order_fulfillment.DeliveryOrderStatus;
import com.wms.enums.order_fulfillment.DeliveryOrderType;
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
    @JsonProperty("created_by_name")
    private String createdByName;
    @JsonProperty("picking_plan_saved_by_name")
    private String pickingPlanSavedByName;
    @JsonProperty("qc_by_name")
    private String qcByName;
    @JsonProperty("approved_by_name")
    private String approvedByName;
    @JsonProperty("warehouse_approved_at")
    private OffsetDateTime warehouseApprovedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
