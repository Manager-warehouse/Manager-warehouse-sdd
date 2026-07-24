package com.wms.service.billing_payment;

import com.wms.dto.request.CorrectionVoucherCreateRequest;
import com.wms.dto.response.CorrectionVoucherResponse;
import com.wms.entity.access_control.User;
import com.wms.enums.billing_payment.CorrectionVoucherReferenceType;
import java.util.List;

public interface CorrectionVoucherService {
    CorrectionVoucherResponse createCorrectionVoucher(CorrectionVoucherCreateRequest request, User actor);

    List<CorrectionVoucherResponse> getCorrectionVouchers(
            CorrectionVoucherReferenceType referenceType, User actor);
}
