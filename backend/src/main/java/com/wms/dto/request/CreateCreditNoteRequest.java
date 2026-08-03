package com.wms.dto.request;


import jakarta.validation.constraints.NotBlank;
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
public class CreateCreditNoteRequest {
    @NotBlank(message = "REASON_REQUIRED: Reason for credit note is mandatory")
    private String reason;
}
