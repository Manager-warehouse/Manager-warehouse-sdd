package com.wms.dto.response;


import com.wms.entity.access_control.*;
import com.wms.entity.audit_trail.*;
import com.wms.entity.billing_payment.*;
import com.wms.entity.dealer_management.*;
import com.wms.entity.document_numbering.*;
import com.wms.entity.driver_management.*;
import com.wms.entity.fleet_management.*;
import com.wms.entity.notification_delivery.*;
import com.wms.entity.order_fulfillment.*;
import com.wms.entity.price_management.*;
import com.wms.entity.product_catalog.*;
import com.wms.entity.stock_control.*;
import com.wms.entity.stock_counting.*;
import com.wms.entity.stock_receiving.*;
import com.wms.entity.supplier_management.*;
import com.wms.entity.user_configuration.*;
import com.wms.entity.warehouse_location.*;
import com.wms.entity.warehouse_transfer.*;
import com.wms.enums.access_control.*;
import com.wms.enums.audit_trail.*;
import com.wms.enums.billing_payment.*;
import com.wms.enums.dealer_management.*;
import com.wms.enums.driver_management.*;
import com.wms.enums.fleet_management.*;
import com.wms.enums.notification_delivery.*;
import com.wms.enums.order_fulfillment.*;
import com.wms.enums.price_management.*;
import com.wms.enums.stock_control.*;
import com.wms.enums.stock_counting.*;
import com.wms.enums.stock_receiving.*;
import com.wms.enums.supplier_management.*;
import com.wms.enums.user_configuration.*;
import com.wms.enums.warehouse_location.*;
import com.wms.enums.warehouse_transfer.*;
import com.wms.entity.warehouse_transfer.InterWarehouseTransfer;
import com.wms.enums.warehouse_transfer.InterWarehouseTransferStatus;
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

        // Outbound QC
        Boolean outboundQcPassed,
        String outboundQcNote,
        String outboundQcPhotoRef,
        Long outboundQcById,
        String outboundQcByName,
        OffsetDateTime outboundQcAt,

        // Load Handover
        String loadHandoverPhotoRef,
        Long loadHandoverById,
        String loadHandoverByName,
        OffsetDateTime loadHandoverAt,

        // Arrival
        OffsetDateTime driverArrivedAt,
        OffsetDateTime arrivalHandoverAt,
        String arrivalHandoverPhotoRef,
        Long arrivalHandoverById,
        String arrivalHandoverByName,

        // Return leg
        OffsetDateTime returnDepartedAt,
        OffsetDateTime returnArrivedAt,
        OffsetDateTime returnArrivalHandoverAt,
        Long returnArrivalHandoverById,
        String returnArrivalHandoverByName,
        String returnPhotoRef,

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

                // Outbound QC
                transfer.getOutboundQcPassed(),
                transfer.getOutboundQcNote(),
                transfer.getOutboundQcPhotoRef(),
                transfer.getOutboundQcBy() == null ? null : transfer.getOutboundQcBy().getId(),
                transfer.getOutboundQcBy() == null ? null : transfer.getOutboundQcBy().getFullName(),
                transfer.getOutboundQcAt(),

                // Load Handover
                transfer.getLoadHandoverPhotoRef(),
                transfer.getLoadHandoverBy() == null ? null : transfer.getLoadHandoverBy().getId(),
                transfer.getLoadHandoverBy() == null ? null : transfer.getLoadHandoverBy().getFullName(),
                transfer.getLoadHandoverAt(),

                // Arrival
                transfer.getDriverArrivedAt(),
                transfer.getArrivalHandoverAt(),
                transfer.getArrivalHandoverPhotoRef(),
                transfer.getArrivalHandoverBy() == null ? null : transfer.getArrivalHandoverBy().getId(),
                transfer.getArrivalHandoverBy() == null ? null : transfer.getArrivalHandoverBy().getFullName(),

                // Return leg
                transfer.getReturnDepartedAt(),
                transfer.getReturnArrivedAt(),
                transfer.getReturnArrivalHandoverAt(),
                transfer.getReturnArrivalHandoverBy() == null ? null : transfer.getReturnArrivalHandoverBy().getId(),
                transfer.getReturnArrivalHandoverBy() == null ? null : transfer.getReturnArrivalHandoverBy().getFullName(),
                transfer.getReturnPhotoRef(),

                items);
    }

    @SuppressWarnings("unchecked")
    public InterWarehouseTransferResponse(
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
            List<?> items) {
        this(id, transferNumber, externalInstructionCode, sourceWarehouseId, sourceWarehouseCode,
                destinationWarehouseId, destinationWarehouseCode, status, tripId, tripNumber,
                vehicleId, vehiclePlate, driverId, driverUserId, driverName, documentDate, plannedDate,
                tripPlannedStartAt, tripPlannedEndAt, tripWarningActive, tripOverdue, tripWarningMessage,
                actualReceivedDate, discrepancyReason, rejectionReason, notes, isReturned, createdAt, updatedAt,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                (List<InterWarehouseTransferItemResponse>) items);
    }
}
