package com.wms.dto.request;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO request từ chối (reject) phiếu kiểm kê — bắt buộc có lý do.
 * Dùng bởi: PUT /api/v1/stocktakes/{id}/reject (StockTakeController.rejectStockTake)
 * → StockTakeService.rejectStockTake()
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class StockTakeRejectRequest {

    @NotBlank(message = "rejection_reason is required")
    @JsonProperty("rejection_reason")
    private String rejectionReason;

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
