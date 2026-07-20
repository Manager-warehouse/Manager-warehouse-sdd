package com.wms.service;

import com.wms.dto.request.DealerCreateRequest;
import com.wms.dto.request.DealerCreditLimitUpdateRequest;
import com.wms.dto.request.DealerCreditStatusUpdateRequest;
import com.wms.dto.request.DealerPaymentTermUpdateRequest;
import com.wms.dto.request.DealerUpdateRequest;
import com.wms.dto.response.DealerResponse;
import com.wms.entity.User;
import java.util.List;

public interface DealerService {
    List<DealerResponse> getAllDealers();
    DealerResponse getDealerById(Long id);
    DealerResponse createDealer(DealerCreateRequest request, User actor);
    DealerResponse updateDealer(Long id, DealerUpdateRequest request, User actor);
    void deactivateDealer(Long id, User actor);
    DealerResponse reactivateDealer(Long id, User actor);
    DealerResponse updateCreditLimit(Long id, DealerCreditLimitUpdateRequest request, User actor);
    DealerResponse updatePaymentTerm(Long id, DealerPaymentTermUpdateRequest request, User actor);
    DealerResponse updateCreditStatus(Long id, DealerCreditStatusUpdateRequest request, User actor);
}
