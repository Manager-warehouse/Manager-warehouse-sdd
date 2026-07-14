package com.wms.service;

import com.wms.dto.request.PaymentReceiptCreateRequest;
import com.wms.dto.response.CreditAgingReportResponse;
import com.wms.dto.response.PaymentReceiptResponse;
import com.wms.entity.User;
import java.util.List;

public interface PaymentReceiptService {
    PaymentReceiptResponse createPaymentReceipt(PaymentReceiptCreateRequest request, User actor);
    List<PaymentReceiptResponse> getPaymentReceipts(Long dealerId, Long periodId, User actor);
    List<CreditAgingReportResponse> getCreditAgingReport(User actor);
    void runDailyOverdueHoldJob();
}
