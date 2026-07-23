package com.wms.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierPaymentOcrResponse {
    private BigDecimal amount;
    private LocalDate paymentDate;
    private Long supplierId;
    private Long supplierInvoiceId;
    private String notes;
    private Double confidenceScore;
}
