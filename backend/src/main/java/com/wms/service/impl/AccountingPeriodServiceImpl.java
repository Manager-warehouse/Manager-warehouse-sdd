package com.wms.service.impl;

import com.wms.dto.request.AccountingPeriodCloseRequest;
import com.wms.dto.request.AccountingPeriodCreateRequest;
import com.wms.dto.response.AccountingPeriodResponse;
import com.wms.entity.*;
import com.wms.enums.*;
import com.wms.exception.DuplicateResourceException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.exception.UnprocessableEntityException;
import com.wms.repository.AccountingPeriodRepository;
import com.wms.service.AccountingPeriodService;
import com.wms.service.AuditLogService;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountingPeriodServiceImpl implements AccountingPeriodService {

    private final AccountingPeriodRepository accountingPeriodRepository;
    private final AuditLogService auditLogService;
    private final EntityManager entityManager;

    public AccountingPeriodServiceImpl(
            AccountingPeriodRepository accountingPeriodRepository,
            AuditLogService auditLogService,
            EntityManager entityManager) {
        this.accountingPeriodRepository = accountingPeriodRepository;
        this.auditLogService = auditLogService;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountingPeriodResponse> getAllPeriods(User actor) {
        requireAccountantOrManager(actor);
        return accountingPeriodRepository.findAllByOrderByStartDateDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AccountingPeriodResponse createPeriod(AccountingPeriodCreateRequest request, User actor) {
        requireAccountantManager(actor);

        if (accountingPeriodRepository.findByPeriodName(request.getPeriodName()).isPresent()) {
            throw new DuplicateResourceException("Accounting period already exists: " + request.getPeriodName());
        }

        AccountingPeriod saved = accountingPeriodRepository.save(buildPeriod(
                YearMonth.parse(request.getPeriodName()), request.getNotes()));

        auditLogService.log(actor, AuditAction.CREATE, "ACCOUNTING_PERIOD",
                saved.getId(), saved.getPeriodName(), null, null,
                Map.of("periodName", saved.getPeriodName(),
                        "startDate", saved.getStartDate(), "endDate", saved.getEndDate()));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public AccountingPeriodResponse closePeriod(Long id, AccountingPeriodCloseRequest request, User actor) {
        requireAccountantManager(actor);

        AccountingPeriod period = accountingPeriodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Accounting period not found with id: " + id));

        if (period.getStatus() == AccountingPeriodStatus.CLOSED) {
            throw new UnprocessableEntityException("Accounting period is already closed");
        }

        // Kiểm tra tính toàn vẹn: không còn chứng từ dở dang trong kỳ
        validateNoPendingDocuments(period.getId());

        OffsetDateTime now = OffsetDateTime.now();
        period.setStatus(AccountingPeriodStatus.CLOSED);
        period.setClosedBy(actor);
        period.setClosedAt(now);
        if (request != null && request.getNotes() != null) {
            period.setNotes(request.getNotes());
        }

        AccountingPeriod saved = accountingPeriodRepository.save(period);

        // Ghi log audit
        auditLogService.log(actor, AuditAction.STATUS_CHANGE, "ACCOUNTING_PERIOD",
                saved.getId(), saved.getPeriodName(),
                null, Map.of("status", "OPEN"),
                Map.of("status", "CLOSED", "closedBy", actor.getId()));

        return toResponse(saved);
    }

    // REQUIRES_NEW so this check can be called from inside a caller's own transaction
    // (e.g. one row of a multi-row Excel import loop) and, if it throws, only this
    // isolated sub-transaction is marked rollback-only/rolled back — not the caller's
    // shared transaction. With plain REQUIRED, catching the exception in the caller
    // does not undo the rollback-only flag the interceptor already set on the joined
    // transaction, and the caller's later commit fails with UnexpectedRollbackException
    // even though the exception was "handled".
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void validateDateInOpenPeriod(LocalDate date) {
        resolveOpenPeriod(date);
    }

    @Override
    @Transactional
    public AccountingPeriod resolveOpenPeriod(LocalDate date) {
        AccountingPeriod period = resolveOrProvisionPeriod(date);
        if (period.getStatus() == AccountingPeriodStatus.CLOSED) {
            throw new UnprocessableEntityException(
                    "PERIOD_CLOSED: Cannot create or modify transactions in a closed accounting period: "
                            + period.getPeriodName());
        }
        return period;
    }

    // Auto-provisions the next calendar-month period on the fly when none exists yet for a
    // future date — a safety net for POST /accounting-periods being forgotten, replacing
    // reliance on manually seeding many months ahead. A missing PAST date is a real data
    // problem and is never silently papered over.
    private AccountingPeriod resolveOrProvisionPeriod(LocalDate date) {
        Optional<AccountingPeriod> existing = accountingPeriodRepository.findPeriodByDate(date);
        if (existing.isPresent()) {
            return existing.get();
        }
        if (date.isBefore(LocalDate.now())) {
            throw new UnprocessableEntityException("No accounting period configured for date: " + date);
        }
        return provisionPeriod(YearMonth.from(date));
    }

    private AccountingPeriod provisionPeriod(YearMonth month) {
        String periodName = month.toString();
        try {
            return accountingPeriodRepository.save(buildPeriod(month, null));
        } catch (DataIntegrityViolationException ex) {
            // Concurrent request already provisioned this month first.
            return accountingPeriodRepository.findByPeriodName(periodName).orElseThrow(() -> ex);
        }
    }

    private AccountingPeriod buildPeriod(YearMonth month, String notes) {
        return AccountingPeriod.builder()
                .periodName(month.toString())
                .startDate(month.atDay(1))
                .endDate(month.atEndOfMonth())
                .status(AccountingPeriodStatus.OPEN)
                .notes(notes)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private static final int PENDING_SAMPLE_LIMIT = 5;

    private void validateNoPendingDocuments(Long periodId) {
        // 1. Receipts (PENDING_RECEIPT, DRAFT, QC_COMPLETED, QC_FAILED) -> Phải APPROVED hoặc REJECTED hoặc RETURNED_TO_SUPPLIER
        checkNoPending(periodId, "Receipt", "r", "receiptNumber",
                "r.status not in ('APPROVED', 'REJECTED', 'RETURNED_TO_SUPPLIER')",
                "pending/unapproved inbound receipts");

        // 2. Delivery Orders (NEW, PICKING, READY_TO_SHIP, IN_TRANSIT, DELIVERED) -> Phải COMPLETED hoặc CANCELLED hoặc RETURNED
        checkNoPending(periodId, "DeliveryOrder", "d", "doNumber",
                "d.status not in ('COMPLETED', 'CANCELLED', 'RETURNED')",
                "pending delivery orders");

        // 3. Transfers (NEW, APPROVED, IN_TRANSIT) -> Phải COMPLETED, COMPLETED_WITH_DISCREPANCY hoặc CANCELLED
        // Entity name is InterWarehouseTransfer (the JPA @Entity for inter_warehouse_transfers);
        // "Transfer" is not a registered entity name and previously made this query fail at parse time.
        checkNoPending(periodId, "InterWarehouseTransfer", "t", "transferNumber",
                "t.status not in ('COMPLETED', 'COMPLETED_WITH_DISCREPANCY', 'CANCELLED')",
                "pending internal warehouse transfers");

        // 4. Stock Takes (DRAFT, IN_PROGRESS, PENDING_APPROVAL) -> Phải APPROVED hoặc CANCELLED
        checkNoPending(periodId, "StockTake", "s", "stockTakeNumber",
                "s.status not in ('APPROVED', 'CANCELLED')",
                "pending stocktakes");

        // 5. Adjustments (Không phải APPROVED)
        checkNoPending(periodId, "Adjustment", "a", "adjustmentNumber",
                "a.approvedBy is null", // Assuming adjustment status is controlled by approval
                "unapproved adjustments");

        // 6. Delivery Orders COMPLETED in this period but with no invoice yet (auto-invoice or
        // backfill never ran). Per spec, UNPAID/PARTIALLY_PAID invoices do NOT block closing —
        // outstanding debt is tracked via the Aging Report in the next open period, not here.
        checkNoPending(periodId, "DeliveryOrder", "d", "doNumber",
                "d.status = 'COMPLETED' and not exists (select 1 from Invoice i where i.deliveryOrder = d)",
                "completed delivery orders have no invoice yet");
    }

    // Reports both a count and a sample of blocking document numbers so the accountant
    // knows exactly which documents to resolve, instead of just "N documents are pending".
    private void checkNoPending(Long periodId, String entityName, String alias, String numberField,
            String whereClause, String docLabel) {
        String from = " from " + entityName + " " + alias
                + " where " + alias + ".accountingPeriod.id = :periodId and " + whereClause;

        Long count = entityManager.createQuery("select count(" + alias + ")" + from, Long.class)
                .setParameter("periodId", periodId)
                .getSingleResult();

        if (count > 0) {
            List<String> sample = entityManager.createQuery(
                            "select " + alias + "." + numberField + from, String.class)
                    .setParameter("periodId", periodId)
                    .setMaxResults(PENDING_SAMPLE_LIMIT)
                    .getResultList();
            throw new UnprocessableEntityException("Cannot close period: " + count + " " + docLabel
                    + " exist in this period (e.g. " + String.join(", ", sample) + ").");
        }
    }

    private void requireAccountantOrManager(User actor) {
        if (actor == null || (actor.getRole() != UserRole.ACCOUNTANT 
                && actor.getRole() != UserRole.ACCOUNTANT_MANAGER 
                && actor.getRole() != UserRole.ADMIN 
                && actor.getRole() != UserRole.CEO)) {
            throw new AccessDeniedException("Access denied: Accountant or Manager privileges required");
        }
    }

    private void requireAccountantManager(User actor) {
        if (actor == null || (actor.getRole() != UserRole.ACCOUNTANT_MANAGER 
                && actor.getRole() != UserRole.ADMIN)) {
            throw new AccessDeniedException("Access denied: Accountant Manager privileges required");
        }
    }

    private AccountingPeriodResponse toResponse(AccountingPeriod entity) {
        return AccountingPeriodResponse.builder()
                .id(entity.getId())
                .periodName(entity.getPeriodName())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .status(entity.getStatus())
                .closedById(entity.getClosedBy() == null ? null : entity.getClosedBy().getId())
                .closedByName(entity.getClosedBy() == null ? null : entity.getClosedBy().getFullName())
                .closedAt(entity.getClosedAt())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
