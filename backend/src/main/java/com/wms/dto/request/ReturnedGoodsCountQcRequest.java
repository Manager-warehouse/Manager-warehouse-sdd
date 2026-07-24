package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnedGoodsCountQcRequest {

    @NotEmpty
    @Valid
    private List<ReturnedGoodsCountQcItemRequest> items;

    private String notes;
}
