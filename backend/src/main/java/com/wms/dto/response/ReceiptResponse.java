package com.wms.dto.response;

import com.wms.entity.Receipt;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReceiptResponse {

    private Long id;
    private String receiptNumber;
    private String type;
    private Long warehouseId;
    private String warehouseName;
    private Long supplierId;
    private String supplierName;
    private String status;
    private String sourceOrderCode;
    private LocalDate documentDate;
    private OffsetDateTime createdAt;

    public static ReceiptResponse from(Receipt r) {
        return ReceiptResponse.builder()
                .id(r.getId())
                .receiptNumber(r.getReceiptNumber())
                .type(r.getType().name())
                .warehouseId(r.getWarehouse().getId())
                .warehouseName(r.getWarehouse().getName())
                .supplierId(r.getSupplier() != null ? r.getSupplier().getId() : null)
                .supplierName(r.getSupplier() != null ? r.getSupplier().getCompanyName() : null)
                .status(r.getStatus().name())
                .sourceOrderCode(r.getSourceOrderCode())
                .documentDate(r.getDocumentDate())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
