package com.wms.service;

import com.wms.dto.request.SupplierCreateRequest;
import com.wms.dto.request.SupplierUpdateRequest;
import com.wms.dto.response.SupplierReceivedOrderDetailResponse;
import com.wms.dto.response.SupplierReceivedOrderResponse;
import com.wms.dto.response.SupplierResponse;
import com.wms.entity.User;
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
