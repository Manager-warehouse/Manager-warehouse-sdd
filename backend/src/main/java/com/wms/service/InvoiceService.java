package com.wms.service;

import com.wms.dto.request.InvoiceCreateRequest;
import com.wms.dto.response.InvoiceResponse;
import com.wms.entity.User;
import java.util.List;

public interface InvoiceService {
    InvoiceResponse createInvoice(InvoiceCreateRequest request, User actor);
    InvoiceResponse getInvoiceById(Long id, User actor);
    List<InvoiceResponse> getInvoices(Long dealerId, String status, User actor);
}
