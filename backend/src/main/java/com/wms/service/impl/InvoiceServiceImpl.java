package com.wms.service.impl;

import com.wms.dto.request.InvoiceCreateRequest;
import com.wms.dto.response.InvoiceResponse;
import com.wms.entity.*;
import com.wms.enums.*;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.*;
import com.wms.service.AutoInvoiceService;
import com.wms.service.InvoiceService;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final DeliveryOrderRepository deliveryOrderRepository;
    private final AutoInvoiceService autoInvoiceService;

    public InvoiceServiceImpl(
            InvoiceRepository invoiceRepository,
            DeliveryOrderRepository deliveryOrderRepository,
            AutoInvoiceService autoInvoiceService) {
        this.invoiceRepository = invoiceRepository;
        this.deliveryOrderRepository = deliveryOrderRepository;
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

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long id, User actor) {
        requireAccountantOrManager(actor);
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
        return toResponse(invoice);
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
                .build();
    }
}
