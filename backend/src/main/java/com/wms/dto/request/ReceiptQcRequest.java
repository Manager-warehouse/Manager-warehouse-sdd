package com.wms.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReceiptQcRequest {

    public enum QcAction { SUBMIT, CONFIRM }

    @NotNull
    @JsonProperty("action")
    private QcAction action;

    /** Bắt buộc khi action = SUBMIT. */
    @Valid
    @JsonProperty("items")
    private List<ReceiptQcItemRequest> items;
}
