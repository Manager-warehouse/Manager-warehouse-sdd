package com.wms.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
