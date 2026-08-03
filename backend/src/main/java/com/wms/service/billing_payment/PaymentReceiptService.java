package com.wms.service.billing_payment;


import com.wms.dto.request.PaymentReceiptCreateRequest;
import com.wms.dto.response.CreditAgingReportResponse;
import com.wms.dto.response.PaymentReceiptResponse;
import com.wms.entity.access_control.User;
import java.util.List;

public interface PaymentReceiptService {
    PaymentReceiptResponse createPaymentReceipt(PaymentReceiptCreateRequest request, User actor);
    List<PaymentReceiptResponse> getPaymentReceipts(Long dealerId, Long periodId, User actor);
    List<CreditAgingReportResponse> getCreditAgingReport(User actor);
    void runDailyOverdueHoldJob();
}
