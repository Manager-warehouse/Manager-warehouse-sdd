package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCreditNoteRequest {
    @NotBlank(message = "REASON_REQUIRED: Reason for credit note is mandatory")
    private String reason;
}
