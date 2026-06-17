package com.wms.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PriceImportResponse {

    private int totalRows;
    private int createdCount;
    private int failedCount;
    private List<CreatedRow> created;
    private List<FailedRow> failed;

    @Getter
    @Builder
    public static class CreatedRow {
        private int row;
        private String productSku;
        private Long priceHistoryId;
    }

    @Getter
    @Builder
    public static class FailedRow {
        private int row;
        private String productSku;
        private String errorCode;
        private String message;
    }
}
