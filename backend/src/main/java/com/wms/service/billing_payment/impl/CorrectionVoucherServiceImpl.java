package com.wms.service.billing_payment.impl;

import com.wms.dto.request.CorrectionVoucherCreateRequest;
import com.wms.dto.response.CorrectionVoucherResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.billing_payment.AccountingPeriod;
import com.wms.entity.billing_payment.Invoice;
import com.wms.entity.billing_payment.PaymentReceipt;
import com.wms.entity.billing_payment.SupplierInvoice;
import com.wms.entity.billing_payment.SupplierPayment;
import com.wms.entity.dealer_management.Dealer;
import com.wms.entity.stock_control.Adjustment;
import com.wms.entity.supplier_management.Supplier;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.billing_payment.AccountingPeriodStatus;
import com.wms.enums.billing_payment.CorrectionVoucherReferenceType;
import com.wms.enums.dealer_management.CreditStatus;
import com.wms.enums.stock_control.AdjustmentType;
import com.wms.exception.ResourceNotFoundException;
import com.wms.exception.UnprocessableEntityException;
import com.wms.repository.AdjustmentRepository;
import com.wms.repository.InvoiceRepository;
import com.wms.repository.PaymentReceiptRepository;
import com.wms.repository.SupplierInvoiceRepository;
import com.wms.repository.SupplierPaymentRepository;
import com.wms.repository.dealer_management.DealerRepository;
import com.wms.repository.supplier_management.SupplierRepository;
import com.wms.service.audit_trail.AuditLogService;
import com.wms.service.billing_payment.AccountingPeriodService;
import com.wms.service.billing_payment.CorrectionVoucherService;
import com.wms.service.user_configuration.SystemConfigService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CorrectionVoucherServiceImpl implements CorrectionVoucherService {

    private final AdjustmentRepository adjustmentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentReceiptRepository paymentReceiptRepository;
    private final SupplierInvoiceRepository supplierInvoiceRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final DealerRepository dealerRepository;
    private final SupplierRepository supplierRepository;
    private final AccountingPeriodService accountingPeriodService;
    private final SystemConfigService systemConfigService;
    private final AuditLogService auditLogService;

    public CorrectionVoucherServiceImpl(
            AdjustmentRepository adjustmentRepository,
            InvoiceRepository invoiceRepository,
            PaymentReceiptRepository paymentReceiptRepository,
            SupplierInvoiceRepository supplierInvoiceRepository,
            SupplierPaymentRepository supplierPaymentRepository,
            DealerRepository dealerRepository,
            SupplierRepository supplierRepository,
            AccountingPeriodService accountingPeriodService,
            SystemConfigService systemConfigService,
            AuditLogService auditLogService) {
        this.adjustmentRepository = adjustmentRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentReceiptRepository = paymentReceiptRepository;
        this.supplierInvoiceRepository = supplierInvoiceRepository;
        this.supplierPaymentRepository = supplierPaymentRepository;
        this.dealerRepository = dealerRepository;
        this.supplierRepository = supplierRepository;
        this.accountingPeriodService = accountingPeriodService;
        this.systemConfigService = systemConfigService;
        this.auditLogService = auditLogService;
    }

    // Resolves the original document by referenceType/referenceId, together with its
    // accounting period and the dealer/supplier whose balance the voucher will adjust.
    private record ResolvedReference(AccountingPeriod originalPeriod, Dealer dealer, Supplier supplier) {
    }

    @Override
    @Transactional
    public CorrectionVoucherResponse createCorrectionVoucher(CorrectionVoucherCreateRequest request, User actor) {
        requireAccountantManager(actor);

        ResolvedReference resolved = resolveReference(request.getReferenceType(), request.getReferenceId());

        // Correction Voucher only exists to fix a document whose own period is already
        // CLOSED. A still-OPEN document should be corrected through its normal flow
        // instead - this endpoint is not a general-purpose edit path.
        if (resolved.originalPeriod() == null || resolved.originalPeriod().getStatus() != AccountingPeriodStatus.CLOSED) {
            throw new UnprocessableEntityException(
                    "ORIGINAL_PERIOD_NOT_CLOSED: Reference document's accounting period is not closed yet");
        }

        // The voucher itself must always land in a currently OPEN period - throws
        // "PERIOD_CLOSED: ..." (same convention used everywhere else in this module)
        // if documentDate falls in a closed period.
        AccountingPeriod targetPeriod = accountingPeriodService.resolveOpenPeriod(request.getDocumentDate());

        OffsetDateTime now = OffsetDateTime.now();
        Adjustment adjustment = Adjustment.builder()
                .adjustmentNumber(generateAdjustmentNumber())
                .type(AdjustmentType.CORRECTION_VOUCHER)
                .amountDelta(request.getAmountDelta())
                .referenceType(request.getReferenceType().name())
                .referenceId(request.getReferenceId())
                .reason(request.getReason())
                .documentDate(request.getDocumentDate())
                .accountingPeriod(targetPeriod)
                .createdBy(actor)
                // Approved at the same step as creation: ACCOUNTANT_MANAGER is already the
                // sole authority over period closing, so there is no separate checker step
                // here, matching how every other AR/AP action in Spec 008 takes effect
                // immediately under a single actor.
                .approvedBy(actor)
                .approvedAt(now)
                .createdAt(now)
                .build();

        adjustment = adjustmentRepository.save(adjustment);

        BigDecimal oldBalance;
        BigDecimal newBalance;
        if (resolved.dealer() != null) {
            Dealer dealer = resolved.dealer();
            oldBalance = dealer.getCurrentBalance() != null ? dealer.getCurrentBalance() : BigDecimal.ZERO;
            newBalance = oldBalance.add(request.getAmountDelta());
            dealer.setCurrentBalance(newBalance);
            dealer.setUpdatedAt(now);
            applyCreditStatusTransition(dealer, newBalance);
            dealerRepository.save(dealer);
        } else {
            Supplier supplier = resolved.supplier();
            oldBalance = supplier.getCurrentBalance() != null ? supplier.getCurrentBalance() : BigDecimal.ZERO;
            newBalance = oldBalance.add(request.getAmountDelta());
            supplier.setCurrentBalance(newBalance);
            supplier.setUpdatedAt(now);
            supplierRepository.save(supplier);
        }

        auditLogService.log(actor, AuditAction.CORRECTION_VOUCHER_CREATE, "ADJUSTMENT",
                adjustment.getId(), adjustment.getAdjustmentNumber(), null,
                Map.of("balance", oldBalance),
                snapshot(adjustment, oldBalance, newBalance));

        return toResponse(adjustment, resolved.dealer(), resolved.supplier());
    }

    // Same balance-transition logic as PaymentReceiptServiceImpl's unlock check and
    // AutoInvoiceServiceImpl's hold check, applied through the same gate so a
    // correction voucher can trigger either direction exactly like an invoice or
    // payment would.
    private void applyCreditStatusTransition(Dealer dealer, BigDecimal newBalance) {
        BigDecimal creditLimit = dealer.getCreditLimit() != null ? dealer.getCreditLimit() : BigDecimal.ZERO;
        if (newBalance.compareTo(creditLimit) > 0) {
            dealer.setCreditStatus(CreditStatus.CREDIT_HOLD);
            return;
        }
        if (dealer.getCreditStatus() == CreditStatus.CREDIT_HOLD) {
            BigDecimal bufferPct = systemConfigService.getDecimalValue("CREDIT_UNLOCK_BUFFER_PCT", new BigDecimal("0.8"));
            BigDecimal unlockThreshold = creditLimit.multiply(bufferPct);
            if (newBalance.compareTo(unlockThreshold) < 0) {
                dealer.setCreditStatus(CreditStatus.ACTIVE);
            }
        }
    }

    private ResolvedReference resolveReference(CorrectionVoucherReferenceType referenceType, Long referenceId) {
        return switch (referenceType) {
            case INVOICE -> {
                Invoice invoice = invoiceRepository.findById(referenceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + referenceId));
                Dealer dealer = dealerRepository.findByIdForUpdate(invoice.getDealer().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Dealer not found with id: " + invoice.getDealer().getId()));
                yield new ResolvedReference(invoice.getAccountingPeriod(), dealer, null);
            }
            case PAYMENT_RECEIPT -> {
                PaymentReceipt paymentReceipt = paymentReceiptRepository.findById(referenceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Payment receipt not found with id: " + referenceId));
                Dealer dealer = dealerRepository.findByIdForUpdate(paymentReceipt.getDealer().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Dealer not found with id: " + paymentReceipt.getDealer().getId()));
                yield new ResolvedReference(paymentReceipt.getAccountingPeriod(), dealer, null);
            }
            case SUPPLIER_INVOICE -> {
                SupplierInvoice supplierInvoice = supplierInvoiceRepository.findById(referenceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Supplier invoice not found with id: " + referenceId));
                Supplier supplier = supplierRepository.findById(supplierInvoice.getSupplier().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + supplierInvoice.getSupplier().getId()));
                yield new ResolvedReference(supplierInvoice.getAccountingPeriod(), null, supplier);
            }
            case SUPPLIER_PAYMENT -> {
                SupplierPayment supplierPayment = supplierPaymentRepository.findById(referenceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Supplier payment not found with id: " + referenceId));
                Supplier supplier = supplierRepository.findById(supplierPayment.getSupplier().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + supplierPayment.getSupplier().getId()));
                yield new ResolvedReference(supplierPayment.getAccountingPeriod(), null, supplier);
            }
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<CorrectionVoucherResponse> getCorrectionVouchers(
            CorrectionVoucherReferenceType referenceType, User actor) {
        requireAccountantOrManager(actor);

        String referenceTypeFilter = referenceType != null ? referenceType.name() : null;
        List<Adjustment> vouchers = adjustmentRepository
                .findByTypeAndOptionalReferenceType(AdjustmentType.CORRECTION_VOUCHER, referenceTypeFilter);

        return vouchers.stream().map(this::toResponseWithLookup).toList();
    }

    // List view re-derives the dealer/supplier from the reference document rather than
    // storing it on the adjustment row itself (see feature-accountant-correction-voucher.md
    // Data Model) - avoids duplicating data that already lives on the original document.
    private CorrectionVoucherResponse toResponseWithLookup(Adjustment adjustment) {
        CorrectionVoucherReferenceType referenceType =
                CorrectionVoucherReferenceType.valueOf(adjustment.getReferenceType());
        Dealer dealer = null;
        Supplier supplier = null;
        switch (referenceType) {
            case INVOICE -> dealer = invoiceRepository.findById(adjustment.getReferenceId())
                    .map(Invoice::getDealer).orElse(null);
            case PAYMENT_RECEIPT -> dealer = paymentReceiptRepository.findById(adjustment.getReferenceId())
                    .map(PaymentReceipt::getDealer).orElse(null);
            case SUPPLIER_INVOICE -> supplier = supplierInvoiceRepository.findById(adjustment.getReferenceId())
                    .map(SupplierInvoice::getSupplier).orElse(null);
            case SUPPLIER_PAYMENT -> supplier = supplierPaymentRepository.findById(adjustment.getReferenceId())
                    .map(SupplierPayment::getSupplier).orElse(null);
        }
        return toResponse(adjustment, dealer, supplier);
    }

    private String generateAdjustmentNumber() {
        return "ADJ-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private Map<String, Object> snapshot(Adjustment adjustment, BigDecimal oldBalance, BigDecimal newBalance) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("referenceType", adjustment.getReferenceType());
        values.put("referenceId", adjustment.getReferenceId());
        values.put("amountDelta", adjustment.getAmountDelta());
        values.put("balanceBefore", oldBalance);
        values.put("balanceAfter", newBalance);
        return values;
    }

    private CorrectionVoucherResponse toResponse(Adjustment adjustment, Dealer dealer, Supplier supplier) {
        return CorrectionVoucherResponse.builder()
                .id(adjustment.getId())
                .adjustmentNumber(adjustment.getAdjustmentNumber())
                .referenceType(CorrectionVoucherReferenceType.valueOf(adjustment.getReferenceType()))
                .referenceId(adjustment.getReferenceId())
                .dealerId(dealer != null ? dealer.getId() : null)
                .dealerName(dealer != null ? dealer.getName() : null)
                .supplierId(supplier != null ? supplier.getId() : null)
                .supplierName(supplier != null ? supplier.getCompanyName() : null)
                .amountDelta(adjustment.getAmountDelta())
                .reason(adjustment.getReason())
                .documentDate(adjustment.getDocumentDate())
                .accountingPeriodId(adjustment.getAccountingPeriod() != null ? adjustment.getAccountingPeriod().getId() : null)
                .accountingPeriodName(adjustment.getAccountingPeriod() != null ? adjustment.getAccountingPeriod().getPeriodName() : null)
                .approvedById(adjustment.getApprovedBy() != null ? adjustment.getApprovedBy().getId() : null)
                .approvedByName(adjustment.getApprovedBy() != null ? adjustment.getApprovedBy().getFullName() : null)
                .approvedAt(adjustment.getApprovedAt())
                .createdAt(adjustment.getCreatedAt())
                .build();
    }

    private void requireAccountantManager(User actor) {
        if (actor == null || (actor.getRole() != UserRole.ACCOUNTANT_MANAGER
                && actor.getRole() != UserRole.ADMIN)) {
            throw new AccessDeniedException("Access denied: Accountant Manager privileges required");
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
}
