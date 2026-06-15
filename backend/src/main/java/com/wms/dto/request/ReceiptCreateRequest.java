package com.wms.dto.request;

import com.wms.enums.SourceChannel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReceiptCreateRequest {

    @NotNull
    private Long supplierId;

    @NotBlank
    private String contactPerson;

    @NotNull
    private Long warehouseId;

    @NotBlank
    private String sourceOrderCode;

    @NotNull
    private SourceChannel sourceChannel;

    @NotNull
    private LocalDate documentDate;

    private String notes;

    @NotEmpty
    @Valid
    private List<ReceiptCreateItemRequest> items;
}
