package com.wms.dto.request.stock_receiving;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviseReceiptRequest {

    @NotNull(message = "EXPECTED_VERSION_REQUIRED")
    private Integer expectedVersion;

    @NotNull(message = "DOCUMENT_DATE_REQUIRED")
    @JsonProperty("documentDate")
    @JsonAlias("document_date")
    private LocalDate documentDate;

    @Valid
    @NotEmpty(message = "RECEIPT_ITEMS_REQUIRED")
    private List<ReviseReceiptItemRequest> items;

    private String notes;
}
