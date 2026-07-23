package com.wms.service.supplier_management;


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
import com.wms.dto.request.supplier_management.SupplierCreateRequest;
import com.wms.dto.request.supplier_management.SupplierUpdateRequest;
import com.wms.dto.response.supplier_management.SupplierReceivedOrderDetailResponse;
import com.wms.dto.response.supplier_management.SupplierReceivedOrderResponse;
import com.wms.dto.response.supplier_management.SupplierResponse;
import com.wms.entity.access_control.User;
import java.util.List;

public interface SupplierService {
    List<SupplierResponse> getAllSuppliers();
    SupplierResponse getSupplierById(Long id);
    SupplierResponse createSupplier(SupplierCreateRequest request, User actor);
    SupplierResponse updateSupplier(Long id, SupplierUpdateRequest request, User actor);
    void deactivateSupplier(Long id, User actor);
    SupplierResponse reactivateSupplier(Long id, User actor);
    List<SupplierReceivedOrderResponse> getReceivedOrders(Long supplierId);
    SupplierReceivedOrderDetailResponse getReceivedOrderDetail(Long supplierId, Long orderId);
}
