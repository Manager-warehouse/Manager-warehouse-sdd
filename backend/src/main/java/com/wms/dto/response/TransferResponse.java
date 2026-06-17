package com.wms.dto.response;

import com.wms.entity.Transfer;
import com.wms.enums.TransferStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record TransferResponse(
        Long id,
        String transferNumber,
        String externalInstructionCode,
        Long sourceWarehouseId,
        String sourceWarehouseCode,
        Long destinationWarehouseId,
        String destinationWarehouseCode,
        TransferStatus status,
        Long tripId,
        String tripNumber,
        LocalDate documentDate,
        LocalDate plannedDate,
        LocalDate actualReceivedDate,
        String discrepancyReason,
        String rejectionReason,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<TransferItemResponse> items) {

    public static TransferResponse from(Transfer transfer, List<TransferItemResponse> items) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getTransferNumber(),
                transfer.getExternalInstructionCode(),
                transfer.getSourceWarehouse().getId(),
                transfer.getSourceWarehouse().getCode(),
                transfer.getDestinationWarehouse().getId(),
                transfer.getDestinationWarehouse().getCode(),
                transfer.getStatus(),
                transfer.getTrip() == null ? null : transfer.getTrip().getId(),
                transfer.getTrip() == null ? null : transfer.getTrip().getTripNumber(),
                transfer.getDocumentDate(),
                transfer.getPlannedDate(),
                transfer.getActualReceivedDate(),
                transfer.getDiscrepancyReason(),
                transfer.getRejectionReason(),
                transfer.getNotes(),
                transfer.getCreatedAt(),
                transfer.getUpdatedAt(),
                items);
    }
}
