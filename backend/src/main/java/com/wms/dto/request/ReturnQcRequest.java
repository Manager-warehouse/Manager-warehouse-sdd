package com.wms.dto.request;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
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
public class ReturnQcRequest {
    @NotNull(message = "VERSION_REQUIRED")
    private Integer expectedVersion;

    @NotEmpty(message = "ITEMS_REQUIRED")
    @Valid
    private List<ReturnQcItemRequest> items;
}
