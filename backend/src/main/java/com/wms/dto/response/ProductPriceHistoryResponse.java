package com.wms.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProductPriceHistoryResponse {
    private Long productId;
    private String productSku;
    private List<PriceHistoryResponse> entries;
}
