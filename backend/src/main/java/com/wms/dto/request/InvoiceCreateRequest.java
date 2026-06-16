package com.wms.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceCreateRequest {

    @NotNull(message = "Delivery Order ID is required")
    @JsonProperty("do_id")
    private Long doId;

    @NotNull(message = "Document date is required")
    @JsonProperty("document_date")
    private LocalDate documentDate;

    private String notes;
}
