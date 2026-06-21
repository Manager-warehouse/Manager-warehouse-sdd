package com.wms.dto.response;

import com.wms.entity.InterWarehouseTransfer;
import com.wms.enums.InterWarehouseTransferStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public record InterWarehouseTransferResponse(
        Long id,
        String transferNumber,
        String externalInstructionCode,
        Long sourceWarehouseId,
        String sourceWarehouseCode,
        Long destinationWarehouseId,
        String destinationWarehouseCode,
        InterWarehouseTransferStatus status,
        Long tripId,
        String tripNumber,
        Long vehicleId,
        String vehiclePlate,
        Long driverId,
        Long driverUserId,
        String driverName,
        LocalDate documentDate,
        LocalDate plannedDate,
        LocalDateTime tripPlannedStartAt,
        LocalDateTime tripPlannedEndAt,
        Boolean tripWarningActive,
        Boolean tripOverdue,
        String tripWarningMessage,
        LocalDate actualReceivedDate,
        String discrepancyReason,
        String rejectionReason,
        String notes,
        Boolean isReturned,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<InterWarehouseTransferItemResponse> items) {

    public static InterWarehouseTransferResponse from(InterWarehouseTransfer transfer,
                                                      List<InterWarehouseTransferItemResponse> items,
                                                      Boolean tripWarningActive,
                                                      Boolean tripOverdue,
                                                      String tripWarningMessage) {
        return new InterWarehouseTransferResponse(
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
                transfer.getTrip() == null ? null : transfer.getTrip().getVehicle().getId(),
                transfer.getTrip() == null ? null : transfer.getTrip().getVehicle().getPlateNumber(),
                transfer.getTrip() == null ? null : transfer.getTrip().getDriver().getId(),
                transfer.getTrip() == null ? null : transfer.getTrip().getDriver().getUser().getId(),
                transfer.getTrip() == null ? null : transfer.getTrip().getDriver().getFullName(),
                transfer.getDocumentDate(),
                transfer.getPlannedDate(),
                transfer.getTrip() == null ? null : transfer.getTrip().getPlannedStartAt(),
                transfer.getTrip() == null ? null : transfer.getTrip().getPlannedEndAt(),
                tripWarningActive,
                tripOverdue,
                tripWarningMessage,
                transfer.getActualReceivedDate(),
                transfer.getDiscrepancyReason(),
                transfer.getRejectionReason(),
                transfer.getNotes(),
                transfer.isReturned(),
                transfer.getCreatedAt(),
                transfer.getUpdatedAt(),
                items);
    }
}
