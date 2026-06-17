package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ReturnCreateRequest {

    @NotNull(message = "WAREHOUSE_ID_REQUIRED")
    private Long warehouseId;

    @NotNull(message = "DEALER_ID_REQUIRED")
    private Long dealerId;

    @NotNull(message = "DELIVERY_ORDER_ID_REQUIRED")
    private Long deliveryOrderId;

    @NotBlank(message = "CONTACT_PERSON_REQUIRED")
    @Size(max = 255, message = "CONTACT_PERSON_TOO_LONG")
    private String contactPerson;

    private LocalDate documentDate;

    @Size(max = 2000, message = "NOTES_TOO_LONG")
    private String notes;

    @Valid
    @NotEmpty(message = "ITEMS_REQUIRED")
    private List<ReturnItemRequest> items;
}
