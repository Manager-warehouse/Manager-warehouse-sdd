package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnQcRequest {
    @NotNull(message = "VERSION_REQUIRED")
    private Integer expectedVersion;

    @NotEmpty(message = "ITEMS_REQUIRED")
    @Valid
    private List<ReturnQcItemRequest> items;
}
