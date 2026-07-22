package com.wms.service.dealer_management;


import com.wms.entity.access_control.*;
import com.wms.entity.audit_trail.*;
import com.wms.entity.billing_payment.*;
import com.wms.entity.dealer_management.*;
import com.wms.entity.document_numbering.*;
import com.wms.entity.driver_management.*;
import com.wms.entity.fleet_management.*;
import com.wms.entity.notification_delivery.*;
import com.wms.entity.order_fulfillment.*;
import com.wms.entity.price_management.*;
import com.wms.entity.product_catalog.*;
import com.wms.entity.stock_control.*;
import com.wms.entity.stock_counting.*;
import com.wms.entity.stock_receiving.*;
import com.wms.entity.supplier_management.*;
import com.wms.entity.user_configuration.*;
import com.wms.entity.warehouse_location.*;
import com.wms.entity.warehouse_transfer.*;
import com.wms.enums.access_control.*;
import com.wms.enums.audit_trail.*;
import com.wms.enums.billing_payment.*;
import com.wms.enums.dealer_management.*;
import com.wms.enums.driver_management.*;
import com.wms.enums.fleet_management.*;
import com.wms.enums.notification_delivery.*;
import com.wms.enums.order_fulfillment.*;
import com.wms.enums.price_management.*;
import com.wms.enums.stock_control.*;
import com.wms.enums.stock_counting.*;
import com.wms.enums.stock_receiving.*;
import com.wms.enums.supplier_management.*;
import com.wms.enums.user_configuration.*;
import com.wms.enums.warehouse_location.*;
import com.wms.enums.warehouse_transfer.*;
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
