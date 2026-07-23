package com.wms.service.billing_payment.impl;
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

import com.wms.dto.request.InvoiceCreateRequest;
import com.wms.dto.response.InvoiceResponse;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.*;
import com.wms.service.billing_payment.AutoInvoiceService;
import com.wms.service.billing_payment.InvoiceService;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final DeliveryOrderRepository deliveryOrderRepository;
    private final DeliveryRepository deliveryRepository;
    private final AutoInvoiceService autoInvoiceService;

    public InvoiceServiceImpl(
            InvoiceRepository invoiceRepository,
            DeliveryOrderRepository deliveryOrderRepository,
            DeliveryRepository deliveryRepository,
            AutoInvoiceService autoInvoiceService) {
        this.invoiceRepository = invoiceRepository;
        this.deliveryOrderRepository = deliveryOrderRepository;
        this.deliveryRepository = deliveryRepository;
        this.autoInvoiceService = autoInvoiceService;
    }

    // Manual backfill entrypoint (POST /api/v1/invoices) — only used when automatic
    // invoicing at delivery-confirmation time failed. Delegates the actual credit-check /
    // period-stamping / numbering logic to AutoInvoiceService so both paths stay in sync.
    @Override
    @Transactional
    public InvoiceResponse createInvoice(InvoiceCreateRequest request, User actor) {
        requireAccountant(actor);

        DeliveryOrder deliveryOrder = deliveryOrderRepository.findById(request.getDoId())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery Order not found with id: " + request.getDoId()));

        Invoice savedInvoice = autoInvoiceService.createBackfillInvoice(deliveryOrder, actor, request.getDocumentDate());

        return toResponse(savedInvoice);
    }

    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoices(Long dealerId, String status, User actor) {
        requireAccountantOrManager(actor);
        List<Invoice> invoices;
        if (dealerId != null) {
            invoices = invoiceRepository.findByDealerIdOrderByCreatedAtDesc(dealerId);
        } else if (status != null && !status.isEmpty()) {
            invoices = invoiceRepository.findByStatusOrderByCreatedAtDesc(InvoiceStatus.valueOf(status.toUpperCase()));
        } else {
            invoices = invoiceRepository.findAll();
        }
        return invoices.stream().map(this::toResponse).toList();
    }

    private void requireAccountant(User actor) {
        if (actor == null || (actor.getRole() != UserRole.ACCOUNTANT 
                && actor.getRole() != UserRole.ADMIN)) {
            throw new AccessDeniedException("Access denied: Accountant role required");
        }
    }

    private void requireAccountantOrManager(User actor) {
        if (actor == null || (actor.getRole() != UserRole.ACCOUNTANT 
                && actor.getRole() != UserRole.ACCOUNTANT_MANAGER 
                && actor.getRole() != UserRole.ADMIN 
                && actor.getRole() != UserRole.CEO)) {
            throw new AccessDeniedException("Access denied: Accountant or Accountant Manager privileges required");
        }
    }

    private InvoiceResponse toResponse(Invoice entity) {
        // Đối chứng bàn giao lấy từ lần giao hàng gần nhất của DO gốc, phục vụ Kế toán
        // đối chiếu hóa đơn tự động sinh với bằng chứng POD thực tế.
        Delivery delivery = deliveryRepository
                .findFirstByDeliveryOrderIdOrderByCreatedAtDesc(entity.getDeliveryOrder().getId())
                .orElse(null);

        return InvoiceResponse.builder()
                .id(entity.getId())
                .invoiceNumber(entity.getInvoiceNumber())
                .doId(entity.getDeliveryOrder().getId())
                .doNumber(entity.getDeliveryOrder().getDoNumber())
                .dealerId(entity.getDealer().getId())
                .dealerName(entity.getDealer().getName())
                .totalAmount(entity.getTotalAmount())
                .issueDate(entity.getIssueDate())
                .dueDate(entity.getDueDate())
                .status(entity.getStatus())
                .createdById(entity.getCreatedBy().getId())
                .createdByName(entity.getCreatedBy().getFullName())
                .documentDate(entity.getDocumentDate())
                .accountingPeriodId(entity.getAccountingPeriod() != null ? entity.getAccountingPeriod().getId() : null)
                .accountingPeriodName(entity.getAccountingPeriod() != null ? entity.getAccountingPeriod().getPeriodName() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .otpVerifiedAt(delivery != null ? delivery.getDeliveredAt() : null)
                .podImageUrl(delivery != null ? delivery.getPodImageUrl() : null)
                .podSignatureUrl(delivery != null ? delivery.getPodSignatureUrl() : null)
                .podTimestamp(delivery != null ? delivery.getPodTimestamp() : null)
                .build();
    }
}
