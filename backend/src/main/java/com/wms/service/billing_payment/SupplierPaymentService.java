package com.wms.service.billing_payment;

import com.wms.dto.request.CreateSupplierPaymentRequest;
import com.wms.dto.response.SupplierPaymentOcrResponse;
import com.wms.dto.response.SupplierPaymentResponse;
import com.wms.entity.access_control.User;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface SupplierPaymentService {
    SupplierPaymentResponse createSupplierPayment(CreateSupplierPaymentRequest request, User actor);
    SupplierPaymentResponse getSupplierPaymentById(Long id, User actor);
    List<SupplierPaymentResponse> getSupplierPayments(Long supplierId, Long invoiceId, User actor);
    SupplierPaymentOcrResponse scanSupplierPaymentOcr(MultipartFile file, User actor);
}
