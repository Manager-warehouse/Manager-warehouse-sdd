package com.wms.dto.response;

import com.wms.enums.TransferStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TransferResponse {
    private Long id;
    private String transferNumber;
    private Long sourceWarehouseId;
    private Long destinationWarehouseId;
    private TransferStatus status;
    private String externalInstructionCode;
    private Long approvedById;
    private OffsetDateTime approvedAt;
    private Long rejectedById;
    private OffsetDateTime rejectedAt;
    private String rejectionReason;
    private Long confirmedById;
    private OffsetDateTime confirmedAt;
    private LocalDate plannedDate;
    private LocalDate actualReceivedDate;
    private String discrepancyReason;
    private Long tripId;
    private LocalDate documentDate;
    private Long accountingPeriodId;
    private String notes;
    private List<TransferItemResponse> items;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
