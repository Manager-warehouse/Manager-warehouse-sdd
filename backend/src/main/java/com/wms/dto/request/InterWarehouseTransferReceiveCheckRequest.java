package com.wms.dto.request;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record InterWarehouseTransferReceiveCheckRequest(
        @NotEmpty(message = "RECEIVE_CHECK_ITEMS_REQUIRED")
        List<@Valid InterWarehouseTransferReceiveCheckItemRequest> items,
        @NotBlank(message = "RECEIVE_QC_PHOTO_REQUIRED") String qcPhotoRef) {}
