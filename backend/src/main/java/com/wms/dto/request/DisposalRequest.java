package com.wms.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisposalRequest {
    @NotBlank(message = "CAUSE_REQUIRED: Cause of damage is mandatory")
    private String cause;

    @JsonProperty("image_url")
    private String imageUrl;
}
