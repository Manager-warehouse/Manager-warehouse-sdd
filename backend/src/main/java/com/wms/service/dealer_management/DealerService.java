package com.wms.service.dealer_management;


import com.wms.dto.request.dealer_management.DealerCreateRequest;
import com.wms.dto.request.dealer_management.DealerCreditLimitUpdateRequest;
import com.wms.dto.request.dealer_management.DealerCreditStatusUpdateRequest;
import com.wms.dto.request.dealer_management.DealerPaymentTermUpdateRequest;
import com.wms.dto.request.dealer_management.DealerUpdateRequest;
import com.wms.dto.response.dealer_management.DealerResponse;
import com.wms.entity.access_control.User;
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
