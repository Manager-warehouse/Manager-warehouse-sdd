package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ReturnCreditNoteRequest {

    @NotBlank(message = "REASON_REQUIRED")
    @Size(max = 2000, message = "REASON_TOO_LONG")
    private String reason;

    private LocalDate documentDate;

    @NotNull(message = "EXPECTED_VERSION_REQUIRED")
    private Integer expectedVersion; // receipt version
}
