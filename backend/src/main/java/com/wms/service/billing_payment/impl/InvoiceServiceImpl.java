package com.wms.service.billing_payment.impl;
import com.wms.dto.request.InvoiceCreateRequest;
import com.wms.dto.response.InvoiceResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.billing_payment.Invoice;
import com.wms.entity.billing_payment.PaymentReceipt;
import com.wms.entity.order_fulfillment.Delivery;
import com.wms.entity.order_fulfillment.DeliveryOrder;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.billing_payment.InvoiceStatus;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.DeliveryOrderRepository;
import com.wms.repository.DeliveryRepository;
import com.wms.repository.InvoiceRepository;
import com.wms.repository.PaymentReceiptRepository;
import com.wms.service.billing_payment.AutoInvoiceService;
import com.wms.service.billing_payment.InvoiceService;
import java.math.BigDecimal;
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
    private final PaymentReceiptRepository paymentReceiptRepository;
    private final UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;

    public InvoiceServiceImpl(
            InvoiceRepository invoiceRepository,
            DeliveryOrderRepository deliveryOrderRepository,
            DeliveryRepository deliveryRepository,
            AutoInvoiceService autoInvoiceService,
            PaymentReceiptRepository paymentReceiptRepository,
            UserWarehouseAssignmentRepository userWarehouseAssignmentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.deliveryOrderRepository = deliveryOrderRepository;
        this.deliveryRepository = deliveryRepository;
        this.autoInvoiceService = autoInvoiceService;
        this.paymentReceiptRepository = paymentReceiptRepository;
        this.userWarehouseAssignmentRepository = userWarehouseAssignmentRepository;
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

        // Enforce warehouse scope check for Accountant role
        if (actor.getRole() == UserRole.ACCOUNTANT) {
            List<Long> assignedWarehouseIds = userWarehouseAssignmentRepository.findWarehouseIdsByUserId(actor.getId());
            if (!assignedWarehouseIds.contains(invoice.getDeliveryOrder().getWarehouse().getId())) {
                throw new AccessDeniedException("Access denied: Warehouse scope mismatch");
            }
        }
        return toResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoices(Long dealerId, String status, Long warehouseId, User actor) {
        requireAccountantOrManager(actor);
        
        List<Long> allowedWarehouseIds = null;
        if (actor.getRole() == UserRole.ACCOUNTANT) {
            allowedWarehouseIds = userWarehouseAssignmentRepository.findWarehouseIdsByUserId(actor.getId());
            if (allowedWarehouseIds.isEmpty()) {
                return List.of();
            }
            if (warehouseId != null) {
                if (!allowedWarehouseIds.contains(warehouseId)) {
                    throw new AccessDeniedException("Access denied: Warehouse scope mismatch");
                }
                allowedWarehouseIds = List.of(warehouseId);
            }
        } else {
            // Admin, CEO, Accountant Manager
            if (warehouseId != null) {
                allowedWarehouseIds = List.of(warehouseId);
            }
        }

        InvoiceStatus invoiceStatus = (status != null && !status.isEmpty()) 
                ? InvoiceStatus.valueOf(status.toUpperCase()) 
                : null;

        List<Invoice> invoices;
        if (allowedWarehouseIds != null) {
            invoices = invoiceRepository.findFilteredInvoicesWithWarehouses(allowedWarehouseIds, dealerId, invoiceStatus);
        } else {
            invoices = invoiceRepository.findFilteredInvoicesWithoutWarehouses(dealerId, invoiceStatus);
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

    private BigDecimal calculatePaidAmount(Long invoiceId) {
        BigDecimal total = BigDecimal.ZERO;
        for (PaymentReceipt payment : paymentReceiptRepository.findByInvoiceId(invoiceId)) {
            total = total.add(payment.getAmount());
        }
        return total;
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
                .paidAmount(calculatePaidAmount(entity.getId()))
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
