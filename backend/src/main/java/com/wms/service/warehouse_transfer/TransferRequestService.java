package com.wms.service.warehouse_transfer;


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
import com.wms.dto.request.TransferRequestCreateRequest;
import com.wms.dto.request.TransferRequestRejectRequest;
import com.wms.dto.request.TransferRequestUpdateRequest;
import com.wms.dto.response.TransferRequestResponse;
import com.wms.dto.response.WarehouseStockLookupResponse;
import com.wms.entity.access_control.User;
import java.util.List;

public interface TransferRequestService {
    List<TransferRequestResponse> getAllRequests(User actor);
    TransferRequestResponse getRequestById(Long id, User actor);
    TransferRequestResponse createRequest(TransferRequestCreateRequest request, User actor);
    TransferRequestResponse updateRequest(Long id, TransferRequestUpdateRequest request, User actor);
    TransferRequestResponse cancelRequest(Long id, User actor);
    TransferRequestResponse submitRequest(Long id, User actor);
    TransferRequestResponse approveRequest(Long id, User actor);
    TransferRequestResponse rejectRequest(Long id, TransferRequestRejectRequest request, User actor);
    TransferRequestResponse convertToTransfer(Long id, User actor);
    List<WarehouseStockLookupResponse> stockLookup(Long productId, User actor);
}
