package com.wms.dto.response;

import com.wms.enums.TransferRequestStatus;
import java.time.OffsetDateTime;
import java.util.List;

public record TransferRequestResponse(
    Long id,
    String requestNumber,
    Long sourceWarehouseId,
    String sourceWarehouseName,
    Long destinationWarehouseId,
    String destinationWarehouseName,
    TransferRequestStatus status,
    Long createdBy,
    String createdByName,
    Long submittedBy,
    String submittedByName,
    OffsetDateTime submittedAt,
    Long approvedBy,
    String approvedByName,
    OffsetDateTime approvedAt,
    Long rejectedBy,
    String rejectedByName,
    OffsetDateTime rejectedAt,
    String rejectionReason,
    String notes,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    List<TransferRequestItemResponse> items
) {}
