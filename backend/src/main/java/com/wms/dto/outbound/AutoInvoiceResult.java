package com.wms.dto.outbound;

import com.wms.enums.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AutoInvoiceResult(
        Long invoiceId,
        String invoiceNumber,
        Long deliveryOrderId,
        Long dealerId,
        BigDecimal totalAmount,
        LocalDate issueDate,
        LocalDate dueDate,
        InvoiceStatus status,
        boolean idempotentReplay,
        List<AutoInvoiceLineResult> lines) {

    public record AutoInvoiceLineResult(
            Long doItemId,
            Long productId,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal lineAmount) {
    }
}
