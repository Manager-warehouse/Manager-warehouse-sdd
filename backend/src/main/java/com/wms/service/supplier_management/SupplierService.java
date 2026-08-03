package com.wms.service.supplier_management;


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
