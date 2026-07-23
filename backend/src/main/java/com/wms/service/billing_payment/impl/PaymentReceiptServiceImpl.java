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

import com.wms.dto.request.PaymentReceiptCreateRequest;
import com.wms.dto.response.CreditAgingReportResponse;
import com.wms.dto.response.PaymentReceiptResponse;
import com.wms.exception.ResourceNotFoundException;
import com.wms.exception.UnprocessableEntityException;
import com.wms.repository.*;
import com.wms.repository.dealer_management.DealerRepository;
import com.wms.service.billing_payment.AccountingPeriodService;
import com.wms.service.audit_trail.AuditLogService;
import com.wms.service.notification_delivery.EmailService;
import com.wms.service.billing_payment.PaymentReceiptService;
import com.wms.service.user_configuration.SystemConfigService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentReceiptServiceImpl implements PaymentReceiptService {

    private static final DateTimeFormatter PAYMENT_NUMBER_DATE = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String PAYMENT_SEQUENCE_KEY = "PAYMENT";

    private final PaymentReceiptRepository paymentReceiptRepository;
    private final InvoiceRepository invoiceRepository;
    private final DealerRepository dealerRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final DocumentSequenceRepository sequenceRepository;
    private final SystemConfigService systemConfigService;
    private final AccountingPeriodService accountingPeriodService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public PaymentReceiptServiceImpl(
            PaymentReceiptRepository paymentReceiptRepository,
            InvoiceRepository invoiceRepository,
            DealerRepository dealerRepository,
            CreditNoteRepository creditNoteRepository,
            AccountingPeriodRepository accountingPeriodRepository,
            DocumentSequenceRepository sequenceRepository,
            SystemConfigService systemConfigService,
            AccountingPeriodService accountingPeriodService,
            AuditLogService auditLogService,
            UserRepository userRepository,
            EmailService emailService) {
        this.paymentReceiptRepository = paymentReceiptRepository;
        this.invoiceRepository = invoiceRepository;
        this.dealerRepository = dealerRepository;
        this.creditNoteRepository = creditNoteRepository;
        this.accountingPeriodRepository = accountingPeriodRepository;
        this.sequenceRepository = sequenceRepository;
        this.systemConfigService = systemConfigService;
        this.accountingPeriodService = accountingPeriodService;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public PaymentReceiptResponse createPaymentReceipt(PaymentReceiptCreateRequest request, User actor) {
        requireAccountant(actor);

        // 1. Validate ngày hạch toán có trong kỳ kế toán đang mở
        accountingPeriodService.validateDateInOpenPeriod(request.getPaymentDate());

        // 2. Tìm kiếm đại lý và hóa đơn (locked for the duration of this transaction so a
        // concurrent invoice/payment for the same dealer can't race on current_balance).
        Dealer dealer = dealerRepository.findByIdForUpdate(request.getDealerId())
                .orElseThrow(() -> new ResourceNotFoundException("Dealer not found with id: " + request.getDealerId()));

        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + request.getInvoiceId()));

        if (!invoice.getDealer().getId().equals(dealer.getId())) {
            throw new UnprocessableEntityException("Invoice does not belong to the specified dealer");
        }

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new UnprocessableEntityException("Invoice is already fully paid");
        }

        // 3. Tính toán dư nợ còn lại của hóa đơn
        // Dư nợ còn lại = totalAmount - sum(payment_receipts.amount)
        BigDecimal totalPaidSoFar = BigDecimal.ZERO;
        List<PaymentReceipt> existingPayments = paymentReceiptRepository.findByDealerIdOrderByCreatedAtDesc(dealer.getId());
        for (PaymentReceipt pr : existingPayments) {
            if (pr.getInvoice().getId().equals(invoice.getId())) {
                totalPaidSoFar = totalPaidSoFar.add(pr.getAmount());
            }
        }
        
        BigDecimal remainingAmount = invoice.getTotalAmount().subtract(totalPaidSoFar);
        if (request.getAmount().compareTo(remainingAmount) > 0) {
            throw new UnprocessableEntityException("Payment amount exceeds invoice remaining balance of " + remainingAmount);
        }

        // 4. Tìm kỳ kế toán cho ngày chứng từ
        AccountingPeriod period = accountingPeriodRepository
                .findPeriodByDateAndStatus(request.getPaymentDate(), AccountingPeriodStatus.OPEN)
                .orElseThrow(() -> new UnprocessableEntityException("No open accounting period found for payment date"));

        // 5. Cập nhật trạng thái hóa đơn
        BigDecimal newPaidAmount = totalPaidSoFar.add(request.getAmount());
        if (newPaidAmount.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }
        invoice.setUpdatedAt(OffsetDateTime.now());
        invoiceRepository.save(invoice);

        // 6. Cập nhật dư nợ đại lý và trạng thái tín dụng
        BigDecimal oldBalance = dealer.getCurrentBalance() != null ? dealer.getCurrentBalance() : BigDecimal.ZERO;
        BigDecimal newBalance = oldBalance.subtract(request.getAmount());
        dealer.setCurrentBalance(newBalance);

        // Mở khóa công nợ tự động nếu dư nợ giảm dưới credit_limit * buffer_pct (CRD-03,
        // business.md) - đây là điều kiện duy nhất, không yêu cầu xử lý hết hóa đơn quá hạn.
        BigDecimal creditLimit = dealer.getCreditLimit() != null ? dealer.getCreditLimit() : BigDecimal.ZERO;
        BigDecimal bufferPct = systemConfigService.getDecimalValue("CREDIT_UNLOCK_BUFFER_PCT", new BigDecimal("0.8"));
        BigDecimal unlockThreshold = creditLimit.multiply(bufferPct);

        if (dealer.getCreditStatus() == CreditStatus.CREDIT_HOLD && newBalance.compareTo(unlockThreshold) < 0) {
            dealer.setCreditStatus(CreditStatus.ACTIVE);
        }
        dealerRepository.save(dealer);

        // 7. Sinh mã phiếu thu tự động
        String paymentNumber = generatePaymentNumber(request.getPaymentDate());

        // 8. Tạo và lưu phiếu thu
        OffsetDateTime now = OffsetDateTime.now();
        PaymentReceipt paymentReceipt = PaymentReceipt.builder()
                .paymentNumber(paymentNumber)
                .dealer(dealer)
                .invoice(invoice)
                .amount(request.getAmount())
                .paymentDate(request.getPaymentDate())
                .paymentMethod(request.getPaymentMethod())
                .createdBy(actor)
                .documentDate(request.getPaymentDate())
                .accountingPeriod(period)
                .notes(request.getNotes())
                .createdAt(now)
                .build();

        PaymentReceipt saved = paymentReceiptRepository.save(paymentReceipt);

        // 9. Ghi log audit
        auditLogService.log(actor, AuditAction.CREATE, "PAYMENT_RECEIPT",
                saved.getId(), saved.getPaymentNumber(),
                invoice.getDeliveryOrder().getWarehouse().getId(), null, snapshot(saved));

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentReceiptResponse> getPaymentReceipts(Long dealerId, Long periodId, User actor) {
        requireAccountantOrManager(actor);
        List<PaymentReceipt> receipts;
        if (dealerId != null) {
            receipts = paymentReceiptRepository.findByDealerIdOrderByCreatedAtDesc(dealerId);
        } else if (periodId != null) {
            receipts = paymentReceiptRepository.findByAccountingPeriodIdOrderByCreatedAtDesc(periodId);
        } else {
            receipts = paymentReceiptRepository.findAll();
        }
        return receipts.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreditAgingReportResponse> getCreditAgingReport(User actor) {
        requireAccountantOrManager(actor);
        List<Dealer> dealers = dealerRepository.findAll();
        List<CreditAgingReportResponse> report = new ArrayList<>();

        LocalDate today = LocalDate.now();

        for (Dealer dealer : dealers) {
            if (!Boolean.TRUE.equals(dealer.getIsActive())) {
                continue;
            }

            BigDecimal currentBalance = dealer.getCurrentBalance() != null ? dealer.getCurrentBalance() : BigDecimal.ZERO;

            // Tìm tất cả hóa đơn chưa thanh toán hoặc thanh toán một phần của đại lý này
            List<Invoice> unpaidInvoices = invoiceRepository.findUnpaidInvoicesByDealer(
                    dealer.getId(), Arrays.asList(InvoiceStatus.UNPAID, InvoiceStatus.PARTIALLY_PAID));

            // Credit notes (Spec 009) only reduce dealer.currentBalance and don't reference a
            // specific invoice, so they'd otherwise never show up in this per-invoice bucketing
            // and the bucket totals would drift from currentBalance. Net the pool against the
            // oldest unpaid invoices first (unpaidInvoices is already ordered by dueDate asc) —
            // read-time only, no CreditNote/Invoice row is mutated.
            BigDecimal creditNotePool = creditNoteRepository.findByDealerId(dealer.getId()).stream()
                    .map(CreditNote::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal inTermAmount = BigDecimal.ZERO;
            BigDecimal overdue1To30 = BigDecimal.ZERO;
            BigDecimal overdue31To60 = BigDecimal.ZERO;
            BigDecimal overdue61To90 = BigDecimal.ZERO;
            BigDecimal overdueOver90 = BigDecimal.ZERO;
            boolean hasHighRiskOverdue = false;

            for (Invoice invoice : unpaidInvoices) {
                // Tính số tiền còn nợ của hóa đơn này
                BigDecimal paidForInvoice = BigDecimal.ZERO;
                List<PaymentReceipt> payments = paymentReceiptRepository.findByDealerIdOrderByCreatedAtDesc(dealer.getId());
                for (PaymentReceipt pr : payments) {
                    if (pr.getInvoice().getId().equals(invoice.getId())) {
                        paidForInvoice = paidForInvoice.add(pr.getAmount());
                    }
                }
                BigDecimal remainingInvoiceDebt = invoice.getTotalAmount().subtract(paidForInvoice);

                if (remainingInvoiceDebt.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                BigDecimal appliedCreditNote = creditNotePool.min(remainingInvoiceDebt);
                creditNotePool = creditNotePool.subtract(appliedCreditNote);
                BigDecimal effectiveDebt = remainingInvoiceDebt.subtract(appliedCreditNote);
                if (effectiveDebt.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                if (invoice.getDueDate().isAfter(today) || invoice.getDueDate().isEqual(today)) {
                    inTermAmount = inTermAmount.add(effectiveDebt);
                } else {
                    long overdueDays = ChronoUnit.DAYS.between(invoice.getDueDate(), today);
                    if (overdueDays >= 1 && overdueDays <= 30) {
                        overdue1To30 = overdue1To30.add(effectiveDebt);
                    } else if (overdueDays >= 31 && overdueDays <= 60) {
                        overdue31To60 = overdue31To60.add(effectiveDebt);
                    } else if (overdueDays >= 61 && overdueDays <= 90) {
                        overdue61To90 = overdue61To90.add(effectiveDebt);
                        hasHighRiskOverdue = true;
                    } else {
                        overdueOver90 = overdueOver90.add(effectiveDebt);
                        hasHighRiskOverdue = true;
                    }
                }
            }

            String riskLevel = hasHighRiskOverdue || dealer.getCreditStatus() == CreditStatus.CREDIT_HOLD ? "HIGH_RISK" : "NORMAL";

            report.add(CreditAgingReportResponse.builder()
                    .dealerId(dealer.getId())
                    .dealerCode(dealer.getCode())
                    .dealerName(dealer.getName())
                    .creditLimit(dealer.getCreditLimit())
                    .currentBalance(currentBalance)
                    .creditStatus(dealer.getCreditStatus())
                    .inTermAmount(inTermAmount)
                    .overdue1To30(overdue1To30)
                    .overdue31To60(overdue31To60)
                    .overdue61To90(overdue61To90)
                    .overdueOver90(overdueOver90)
                    .riskLevel(riskLevel)
                    .build());
        }

        return report;
    }

    @Override
    @Transactional
    public void runDailyOverdueHoldJob() {
        LocalDate today = LocalDate.now();
        int maxOverdueDays = systemConfigService.getIntValue("CREDIT_HOLD_OVERDUE_DAYS", 30);

        List<Invoice> unpaidInvoices = invoiceRepository.findByStatusOrderByCreatedAtDesc(InvoiceStatus.UNPAID);
        List<Invoice> partialInvoices = invoiceRepository.findByStatusOrderByCreatedAtDesc(InvoiceStatus.PARTIALLY_PAID);

        List<Invoice> allUnpaid = new ArrayList<>();
        allUnpaid.addAll(unpaidInvoices);
        allUnpaid.addAll(partialInvoices);

        for (Invoice invoice : allUnpaid) {
            if (invoice.getDueDate().isBefore(today)) {
                long overdueDays = ChronoUnit.DAYS.between(invoice.getDueDate(), today);
                if (overdueDays > maxOverdueDays) {
                    // Locked so this doesn't race with a concurrent payment unlocking the
                    // same dealer's credit at the same time.
                    Dealer dealer = dealerRepository.findByIdForUpdate(invoice.getDealer().getId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Dealer not found with id: " + invoice.getDealer().getId()));
                    if (dealer.getCreditStatus() == CreditStatus.ACTIVE) {
                        dealer.setCreditStatus(CreditStatus.CREDIT_HOLD);
                        dealerRepository.save(dealer);

                        // Ghi log hệ thống tự động khóa
                        User systemUser = User.builder().id(1L).fullName("System Job").role(UserRole.ADMIN).build(); // Seeded admin or mock system user
                        auditLogService.log(systemUser, AuditAction.STATUS_CHANGE, "DEALER_CREDIT",
                                dealer.getId(), dealer.getCode(),
                                null, Map.of("creditStatus", "ACTIVE"),
                                Map.of("creditStatus", "CREDIT_HOLD", "reason", "Overdue invoice " + invoice.getInvoiceNumber() + " by " + overdueDays + " days"));

                        alertAccountantManagers(dealer, invoice, overdueDays);
                    }
                }
            }
        }
    }

    private void alertAccountantManagers(Dealer dealer, Invoice invoice, long overdueDays) {
        List<User> managers = userRepository.findByRole(UserRole.ACCOUNTANT_MANAGER);
        for (User manager : managers) {
            if (manager.getEmail() != null && !manager.getEmail().isBlank()) {
                emailService.sendCreditHoldAlert(manager.getEmail(), dealer.getCode(), dealer.getName(),
                        invoice.getInvoiceNumber(), overdueDays);
            }
        }
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

    private String generatePaymentNumber(LocalDate docDate) {
        String datePart = docDate.format(PAYMENT_NUMBER_DATE);
        DocumentSequence sequence = sequenceRepository
                .findBySequenceKeyForUpdate(PAYMENT_SEQUENCE_KEY)
                .orElseGet(() -> {
                    DocumentSequence newSeq = new DocumentSequence();
                    newSeq.setSequenceKey(PAYMENT_SEQUENCE_KEY);
                    newSeq.setNextValue(1L);
                    newSeq.setUpdatedAt(OffsetDateTime.now());
                    return sequenceRepository.save(newSeq);
                });
        long value = sequence.getNextValue();
        sequence.setNextValue(value + 1);
        sequence.setUpdatedAt(OffsetDateTime.now());
        sequenceRepository.save(sequence);
        return "PAY-" + datePart + "-" + String.format("%06d", value);
    }

    private PaymentReceiptResponse toResponse(PaymentReceipt entity) {
        return PaymentReceiptResponse.builder()
                .id(entity.getId())
                .paymentNumber(entity.getPaymentNumber())
                .dealerId(entity.getDealer().getId())
                .dealerName(entity.getDealer().getName())
                .invoiceId(entity.getInvoice().getId())
                .invoiceNumber(entity.getInvoice().getInvoiceNumber())
                .amount(entity.getAmount())
                .paymentDate(entity.getPaymentDate())
                .paymentMethod(entity.getPaymentMethod())
                .createdById(entity.getCreatedBy().getId())
                .createdByName(entity.getCreatedBy().getFullName())
                .documentDate(entity.getDocumentDate())
                .accountingPeriodId(entity.getAccountingPeriod() != null ? entity.getAccountingPeriod().getId() : null)
                .accountingPeriodName(entity.getAccountingPeriod() != null ? entity.getAccountingPeriod().getPeriodName() : null)
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private Map<String, Object> snapshot(PaymentReceipt entity) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("paymentNumber", entity.getPaymentNumber());
        values.put("dealerId", entity.getDealer().getId());
        values.put("invoiceId", entity.getInvoice().getId());
        values.put("amount", entity.getAmount());
        values.put("paymentMethod", entity.getPaymentMethod().name());
        return values;
    }
}
