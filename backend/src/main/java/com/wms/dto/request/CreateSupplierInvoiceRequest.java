package com.wms.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSupplierInvoiceRequest {

    @NotNull(message = "Receipt ID is required")
    private Long receiptId;

    @NotBlank(message = "Supplier invoice number is required")
    private String supplierInvoiceNumber;

    @NotNull(message = "Document date is required")
    private LocalDate documentDate;

    private LocalDate dueDate;

    private String notes;
}
