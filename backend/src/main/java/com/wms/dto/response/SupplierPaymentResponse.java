package com.wms.dto.response;

import com.wms.enums.billing_payment.PaymentMethod;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierPaymentResponse {
    private Long id;
    private String paymentNumber;
    private Long supplierId;
    private String supplierName;
    private Long supplierInvoiceId;
    private String invoiceNumber;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private PaymentMethod paymentMethod;
    private Long accountingPeriodId;
    private LocalDate documentDate;
    private String notes;
    private String createdByName;
    private OffsetDateTime createdAt;
}
