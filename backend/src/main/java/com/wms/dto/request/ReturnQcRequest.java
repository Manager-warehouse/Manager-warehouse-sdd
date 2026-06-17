package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReturnQcRequest {

    @NotNull(message = "EXPECTED_VERSION_REQUIRED")
    private Integer expectedVersion;

    @Valid
    @NotEmpty(message = "ITEMS_REQUIRED")
    private List<ReturnQcItemRequest> items;
}
