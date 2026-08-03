package com.wms.dto.request;


import com.fasterxml.jackson.annotation.JsonProperty;
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
public class DisposalRequest {
    @NotBlank(message = "CAUSE_REQUIRED: Cause of damage is mandatory")
    private String cause;

    @JsonProperty("image_url")
    private String imageUrl;
}
