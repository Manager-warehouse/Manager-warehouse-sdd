# Quickstart: Accountant Receivable Payment

## Goal

Implement automatic invoice and receivable creation after full Driver Mobile POD confirmation while preserving idempotency, Dealer balance integrity, and the shared delivery-confirmation transaction.

## Suggested implementation order

1. Add or extend persistence for invoice idempotency by Delivery Order and invoice line detail if missing.
2. Extend repositories to load Delivery Order with dealer and items, find invoice by Delivery Order, and update Dealer balance in the same transaction.
3. Add an internal `AutoInvoiceService` invoked by successful driver confirm-delivery.
4. Implement service methods that:
   - validate Delivery Order is ready for full delivery confirmation
   - return existing invoice idempotently when present
   - calculate line amounts from `DeliveryOrderItem.unit_price`
   - create invoice with `UNPAID`, `issue_date = today`, and `due_date = today + 30 days`
   - increase Dealer `current_balance` exactly once
   - participate in rollback with delivery confirmation
5. Update driver confirm-delivery response if useful so the mobile/API caller can see the invoice id or invoice number returned from the internal flow.

## Internal flow walkthrough

### 1. Full delivery confirmation triggers invoice creation

Trigger:

```text
Driver confirms Delivery Order 101 with valid POD + OTP.
```

Expected result:

- Validate Delivery Order 101 is `IN_TRANSIT`.
- Validate confirmation is for full Delivery Order delivery.
- Check whether an invoice already exists for Delivery Order 101.
- If no invoice exists, calculate total from Delivery Order items.
- Create invoice with `UNPAID` status.
- Create invoice line detail if supported by the implementation.
- Increase Dealer `current_balance` by invoice total.
- Move Delivery Order to `COMPLETED` as part of the surrounding delivery-confirmation transaction.
- Link the financial effect to `CONFIRM_DELIVERY` audit context.

### 2. Idempotent retry after invoice exists

Trigger:

```text
Successful confirmation is retried for Delivery Order 101 after invoice creation already committed.
```

Expected result:

- Return existing invoice result.
- Do not insert another invoice.
- Do not increase Dealer `current_balance` again.
- Do not create invoices for other Delivery Orders in the same trip.

### 3. Technical failure rolls back delivery confirmation

Trigger:

```text
Invoice or Dealer balance persistence fails before transaction commit.
```

Expected result:

- Roll back delivery-confirmation transaction.
- Keep Delivery Order in `IN_TRANSIT`.
- Do not persist invoice.
- Do not change Dealer `current_balance`.
- Allow retry after the failure is resolved.

## Required tests

- Service test: auto-create invoice from full Delivery Order confirmation using item `unit_price` snapshots.
- Service test: due date is exactly 30 calendar days after issue date.
- Service test: existing invoice returns idempotently and does not change Dealer balance.
- Service test: only the confirmed Delivery Order in a multi-DO trip is invoiced.
- Service test: missing item `unit_price` or partial delivery is rejected.
- Service test: simulated invoice persistence failure rolls back Delivery Order completion and Dealer balance change.
- Integration-style service test: driver confirm-delivery invokes auto-invoice in the same transaction.

## Definition of done reminders

- No user-facing endpoint belongs to this feature.
- No notifications are sent by this feature.
- No payment collection, approval, receivable deduction, due-date extension, or `CLOSED` transition is implemented here.
- Dealer balance must increase exactly once per Delivery Order invoice.
