package com.wms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PriceImportResponse {

    @JsonProperty("total_rows")
    private int totalRows;

    @JsonProperty("created_count")
    private int createdCount;

    @JsonProperty("failed_count")
    private int failedCount;

    private List<CreatedRow> created;
    private List<FailedRow> failed;

    @Getter
    @Builder
    public static class CreatedRow {
        private int row;

        @JsonProperty("product_sku")
        private String productSku;

        @JsonProperty("price_history_id")
        private Long priceHistoryId;
    }

    @Getter
    @Builder
    public static class FailedRow {
        private int row;

        @JsonProperty("product_sku")
        private String productSku;

        @JsonProperty("error_code")
        private String errorCode;

        private String message;
    }
}
