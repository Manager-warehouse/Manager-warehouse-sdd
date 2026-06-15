package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReceiptRtvRequest {

    @NotBlank
    private String reason;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private LocalDate documentDate;
}
