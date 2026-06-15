package com.wms.dto.response;

import com.wms.entity.Receipt;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReceiptDetailResponse {

    private Long id;
    private String receiptNumber;
    private String type;
    private String sourceOrderCode;
    private String sourceChannel;
    private String contactPerson;
    private Long warehouseId;
    private String warehouseName;
    private Long supplierId;
    private String supplierName;
    private String status;
    private String rejectionReason;
    private Long approvedById;
    private OffsetDateTime approvedAt;
    private LocalDate documentDate;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<ReceiptItemResponse> items;

    public static ReceiptDetailResponse from(Receipt r, List<ReceiptItemResponse> items) {
        return ReceiptDetailResponse.builder()
                .id(r.getId())
                .receiptNumber(r.getReceiptNumber())
                .type(r.getType().name())
                .sourceOrderCode(r.getSourceOrderCode())
                .sourceChannel(r.getSourceChannel() != null ? r.getSourceChannel().name() : null)
                .contactPerson(r.getContactPerson())
                .warehouseId(r.getWarehouse().getId())
                .warehouseName(r.getWarehouse().getName())
                .supplierId(r.getSupplier() != null ? r.getSupplier().getId() : null)
                .supplierName(r.getSupplier() != null ? r.getSupplier().getCompanyName() : null)
                .status(r.getStatus().name())
                .rejectionReason(r.getRejectionReason())
                .approvedById(r.getApprovedBy() != null ? r.getApprovedBy().getId() : null)
                .approvedAt(r.getApprovedAt())
                .documentDate(r.getDocumentDate())
                .notes(r.getNotes())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .items(items)
                .build();
    }
}
