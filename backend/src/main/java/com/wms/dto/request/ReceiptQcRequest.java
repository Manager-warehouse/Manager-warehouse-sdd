package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReceiptQcRequest {

    public enum QcAction { SUBMIT, CONFIRM }

    @NotNull
    private QcAction action;

    /** Bắt buộc khi action = SUBMIT. */
    @Valid
    private List<ReceiptQcItemRequest> items;
}
