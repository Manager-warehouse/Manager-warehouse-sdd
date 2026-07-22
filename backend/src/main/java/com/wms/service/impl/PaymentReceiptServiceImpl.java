@ -10,6 +10,7 @@ import com.wms.exception.UnprocessableEntityException;
import com.wms.repository.*;
import com.wms.service.AccountingPeriodService;
import com.wms.service.AuditLogService;
import com.wms.service.PaymentReceiptService;
import com.wms.service.SystemConfigService;
import java.math.BigDecimal;
@ -41,6 +42,8 @@ public class PaymentReceiptServiceImpl implements PaymentReceiptService {
    private final SystemConfigService systemConfigService;
    private final AccountingPeriodService accountingPeriodService;
    private final AuditLogService auditLogService;

    public PaymentReceiptServiceImpl(
            PaymentReceiptRepository paymentReceiptRepository,
@ -51,7 +54,9 @@ public class PaymentReceiptServiceImpl implements PaymentReceiptService {
            DocumentSequenceRepository sequenceRepository,
            SystemConfigService systemConfigService,
            AccountingPeriodService accountingPeriodService,
            AuditLogService auditLogService) {
        this.paymentReceiptRepository = paymentReceiptRepository;
        this.invoiceRepository = invoiceRepository;
        this.dealerRepository = dealerRepository;
@ -61,6 +66,8 @@ public class PaymentReceiptServiceImpl implements PaymentReceiptService {
        this.systemConfigService = systemConfigService;
        this.accountingPeriodService = accountingPeriodService;
        this.auditLogService = auditLogService;
    }

    @Override
@ -122,15 +129,13 @@ public class PaymentReceiptServiceImpl implements PaymentReceiptService {
        BigDecimal newBalance = oldBalance.subtract(request.getAmount());
        dealer.setCurrentBalance(newBalance);

        // Mở khóa công nợ tự động nếu dư nợ giảm dưới credit_limit * buffer_pct VÀ không còn
        // hóa đơn nào quá hạn - cả hai điều kiện phải thỏa mãn, không chỉ riêng số dư.
        BigDecimal creditLimit = dealer.getCreditLimit() != null ? dealer.getCreditLimit() : BigDecimal.ZERO;
        BigDecimal bufferPct = systemConfigService.getDecimalValue("CREDIT_UNLOCK_BUFFER_PCT", new BigDecimal("0.8"));
        BigDecimal unlockThreshold = creditLimit.multiply(bufferPct);

        if (dealer.getCreditStatus() == CreditStatus.CREDIT_HOLD
                && newBalance.compareTo(unlockThreshold) < 0
                && !hasOverdueInvoice(dealer)) {
            dealer.setCreditStatus(CreditStatus.ACTIVE);
        }
        dealerRepository.save(dealer);
@ -308,17 +313,22 @@ public class PaymentReceiptServiceImpl implements PaymentReceiptService {
                                dealer.getId(), dealer.getCode(),
                                null, Map.of("creditStatus", "ACTIVE"),
                                Map.of("creditStatus", "CREDIT_HOLD", "reason", "Overdue invoice " + invoice.getInvoiceNumber() + " by " + overdueDays + " days"));
                    }
                }
            }
        }
    }

    private boolean hasOverdueInvoice(Dealer dealer) {
        int overdueDays = systemConfigService.getIntValue("CREDIT_HOLD_OVERDUE_DAYS", 30);
        LocalDate overdueThreshold = LocalDate.now().minusDays(overdueDays);
        return invoiceRepository.existsByDealerIdAndStatusInAndDueDateBefore(
                dealer.getId(), List.of(InvoiceStatus.UNPAID, InvoiceStatus.PARTIALLY_PAID), overdueThreshold);
    }

    private void requireAccountant(User actor) {
