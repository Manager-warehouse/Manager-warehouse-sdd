# Research: Accountant Receivable Payment

## Decision: Trigger invoice creation only from successful driver confirm-delivery

**Rationale**: The feature has no user-facing endpoint and belongs to the transactional completion of a full Delivery Order delivery. Invoking auto-invoice logic from the successful driver confirmation flow ensures invoice, receivable, inventory, delivery attempt, and Delivery Order status updates remain atomic.

**Alternatives considered**: A separate manual invoice endpoint was rejected because the spec explicitly marks manual invoice creation out of scope.

## Decision: Make invoice creation idempotent by Delivery Order

**Rationale**: Driver confirmation may be retried after transient errors. A unique invoice lookup or constraint on `do_id` prevents duplicate invoices and prevents Dealer `current_balance` from being increased twice.

**Alternatives considered**: Creating a new invoice for every successful confirmation call was rejected because the spec requires exactly one invoice per Delivery Order.

## Decision: Calculate invoice amount from Delivery Order item snapshots

**Rationale**: Storekeeper planning captures `unit_price` on `delivery_order_items`. Auto-invoice must use this snapshot and delivered/requested full quantity, not the current product price, so the invoice reflects the commercial agreement at picking time.

**Alternatives considered**: Re-querying product price at delivery time was rejected because it can drift from the price used to reserve and approve the order.

## Decision: Use backend local date for issue date and due date = issue date + 30 calendar days

**Rationale**: The spec requires real-time backend local date and a fixed 30-day due date for this feature, regardless of Dealer payment-term settings used elsewhere.

**Alternatives considered**: Using dealer `paymentTermDays` was rejected for this feature because the requirement says exactly 30 calendar days.

## Decision: Treat Dealer `current_balance` as the receivable balance for Sprint 1

**Rationale**: The existing model stores `dealers.current_balance` and no separate receivable ledger entity appears in the current codebase. Increasing this balance in the same transaction satisfies the Sprint 1 receivable effect while leaving richer payment flows to separate finance specs.

**Alternatives considered**: Introducing a full receivable ledger was deferred because the feature only requires automatic balance increase and explicitly excludes payment processing.

## Decision: Add invoice lines if item-level invoice detail is missing

**Rationale**: The spec says invoice lines come from Delivery Order items. If the implementation only has invoice headers, a small `invoice_lines` table preserves product, quantity, unit price, and line amount traceability without changing the public scope.

**Alternatives considered**: Storing only invoice total was considered minimal but weakens auditability and item-level reconciliation.

## Decision: Keep notifications and payment mutations out of this feature

**Rationale**: The feature explicitly excludes accountant notifications, due-date extension, payment collection, payment approval, receivable deduction, and Delivery Order `CLOSED` transition. Keeping those absent reduces hidden coupling and makes the internal trigger narrow.

**Alternatives considered**: Sending accountant notification immediately after invoice creation was rejected because notification belongs to a separate flow.
