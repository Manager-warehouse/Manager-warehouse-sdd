# Audit Logging Integration Guide

This guide details how to integrate audit logging into any warehouse business service in the WMS system.

---

## 1. Core Principles

1. **Immutability**: Audit logs are read-only. There are no delete or update methods.
2. **Diff-Only JSONB**: Only record the fields that changed. This minimizes database storage and clarifies the changes.
3. **Sensitive Field Exclusion**: Keep credential field names (e.g., `passwordHash`, tokens) but omit before/after values.
4. **No Read-Only Audit**: Do not create audit entries for read-only view, search, filter, report, dashboard, or export actions in Sprint 1.
5. **Nullable Entity References**: Use null `entityType`/`entityId` when an action has no natural affected entity row.

---

## 2. Integration Steps

### Step 1: Inject AuditLogService

Inject `AuditLogService` into your Spring `@Service` bean:

```java
@Service
public class TransferService {

    private final AuditLogService auditLogService;

    public TransferService(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }
}
```

### Step 2: Log Document Creation

When a document (e.g. Receipt, Issue) is created, log the operation. Use `null` for `oldValue`:

```java
@Transactional
public Receipt createReceipt(Receipt receipt, User creator) {
    // 1. Business logic to persist entity
    receiptRepository.save(receipt);

    // 2. Build the newValue map containing key details
    Map<String, Object> newValue = Map.of(
            "receiptNumber", receipt.getReceiptNumber(),
            "status", ReceiptStatus.PENDING_RECEIPT.name(),
            "warehouseId", receipt.getWarehouse().getId()
    );

    // 3. Log the creation
    auditLogService.log(
            creator,
            AuditAction.CREATE,
            "RECEIPT",
            receipt.getId(),
            receipt.getReceiptNumber(),
            receipt.getWarehouse().getId(),
            null, // oldValue is null for creation
            newValue
    );

    return receipt;
}
```

### Step 3: Log Updates & State Transitions

When modifying an entity, calculate the diff (or record the state changes) and call the logger. Only include the changed fields:

```java
@Transactional
public void approveReceipt(Receipt receipt, User approver) {
    ReceiptStatus oldStatus = receipt.getStatus();
    
    // Perform state transition
    receipt.setStatus(ReceiptStatus.APPROVED);
    receiptRepository.save(receipt);

    // Construct the diff maps
    Map<String, Object> oldValue = Map.of("status", oldStatus.name());
    Map<String, Object> newValue = Map.of("status", ReceiptStatus.APPROVED.name());

    auditLogService.log(
            approver,
            AuditAction.APPROVE,
            "RECEIPT",
            receipt.getId(),
            receipt.getReceiptNumber(),
            receipt.getWarehouse().getId(),
            oldValue,
            newValue
    );
}
```

---

## 3. Special Case: Transfers (Dual Entries)

For **Inter-Warehouse Transfers**, you must log **two** entries:
1. One for the **Source Warehouse** (outbound operation context).
2. One for the **Destination Warehouse** (inbound operation context).

This ensures that the warehouse managers of both locations can see the action in their respective warehouse logs.

```java
@Transactional
public void processTransfer(Transfer transfer, User actor) {
    // Business logic...
    
    Map<String, Object> oldValue = Map.of("status", "PLANNED");
    Map<String, Object> newValue = Map.of("status", "IN_TRANSIT");

    // Entry 1: Source Warehouse Log
    auditLogService.log(
            actor,
            AuditAction.UPDATE,
            "TRANSFER",
            transfer.getId(),
            transfer.getTransferNumber(),
            transfer.getSourceWarehouse().getId(),
            oldValue,
            newValue
    );

    // Entry 2: Destination Warehouse Log
    auditLogService.log(
            actor,
            AuditAction.UPDATE,
            "TRANSFER",
            transfer.getId(),
            transfer.getTransferNumber(),
            transfer.getDestinationWarehouse().getId(),
            oldValue,
            newValue
    );
}
```

---

## 4. Non-Entity Events

Authentication state-changing actions such as login/logout may be logged without
a business entity reference:

```java
auditLogService.log(
        actor,
        AuditAction.LOGIN,
        null,
        null,
        null,
        null,
        Map.of(),
        Map.of()
);
```

The persisted row must still include actor, actor role, action, timestamp, and
description. `entity_type` and `entity_id` may remain null.

---

## 5. Read-Only Screens

Do not call `AuditLogService` from read-only list/detail/report/dashboard/export
flows. Audit logging is reserved for mutations and authentication
state-changing actions in Sprint 1.
