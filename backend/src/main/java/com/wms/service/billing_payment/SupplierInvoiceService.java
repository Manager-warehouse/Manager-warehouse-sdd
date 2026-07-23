package com.wms.service.billing_payment;

import com.wms.dto.request.CreateSupplierInvoiceRequest;
import com.wms.dto.response.SupplierInvoiceResponse;
import com.wms.entity.access_control.User;
import java.util.List;

public interface SupplierInvoiceService {
    SupplierInvoiceResponse createSupplierInvoice(CreateSupplierInvoiceRequest request, User actor);
    SupplierInvoiceResponse getSupplierInvoiceById(Long id, User actor);
    List<SupplierInvoiceResponse> getSupplierInvoices(Long supplierId, String status, User actor);
}
