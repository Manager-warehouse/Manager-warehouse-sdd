package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmDeliveryRequest {

    @NotBlank
    @Pattern(regexp = "^[0-9]{6}$")
    private String otp;

    @Size(max = 1000)
    private String notes;
}
