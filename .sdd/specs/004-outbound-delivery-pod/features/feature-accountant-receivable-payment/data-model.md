# Data Model: Accountant Receivable Payment

## Invoice

**Purpose**: One automatic invoice created for one successfully confirmed Delivery Order.

**Fields used/updated**

- `id`
- `invoice_number`
- `do_id`
- `dealer_id`
- `total_amount`
- `issue_date`
- `due_date`
- `status`
- `created_by`
- `document_date`
- `accounting_period_id`
- `created_at`
- `updated_at`

**Validation rules**

- Exactly one invoice per Delivery Order.
- Existing invoice is returned idempotently and must not increase Dealer balance again.
- `issue_date` is backend local date at creation.
- `due_date = issue_date + 30 calendar days`.
- `status = UNPAID` at creation.
- Invoice is created only after full POD + OTP confirmation succeeds.

## InvoiceLine

**Purpose**: Item-level invoice detail derived from Delivery Order items.

**Fields**

- `id`
- `invoice_id`
- `do_item_id`
- `product_id`
- `quantity`
- `unit_price`
- `line_amount`

**Validation rules**

- One line per invoiced Delivery Order item.
- `quantity` is the full delivered quantity for Sprint 1, normally `requested_qty` or validated `issued_qty`.
- `unit_price` comes from `DeliveryOrderItem.unit_price`.
- `line_amount = quantity * unit_price`.
- Sum of line amounts equals `Invoice.total_amount`.

## Dealer

**Purpose**: Receivable owner whose current balance increases when invoice is created.

**Fields used/updated**

- `id`
- `current_balance`
- `updated_at`

**Validation rules**

- Dealer belongs to the confirmed Delivery Order.
- `current_balance` increases by invoice `total_amount` only when a new invoice is created.
- Idempotent existing-invoice result must not change balance.

## DeliveryOrder

**Purpose**: Outbound order whose successful confirmation triggers automatic invoicing.

**Fields used/updated**

- `id`
- `dealer_id`
- `status`
- `updated_at`

**Validation rules**

- Must be `IN_TRANSIT` before successful confirmation.
- Full delivery confirmation moves it to `COMPLETED`.
- Only the confirmed Delivery Order is invoiced; other Delivery Orders in the same trip are untouched.

## DeliveryOrderItem

**Purpose**: Source for invoice line quantity and price.

**Fields used**

- `id`
- `delivery_order_id`
- `product_id`
- `requested_qty`
- `issued_qty`
- `unit_price`

**Validation rules**

- `unit_price` must be present before invoice creation.
- Sprint 1 full delivery means invoice quantity equals requested/full delivered quantity.
- Partial delivery invoice is rejected because partial delivery is out of scope.

## Delivery / DeliveryOtpAttempt

**Purpose**: Proof that POD and OTP confirmation succeeded before invoice creation.

**Fields used**

- Delivery `status`
- Delivery `otp_verified_at`
- Delivery POD URLs
- OTP status and consumed timestamp

**Validation rules**

- Invoice creation runs only as part of successful confirm-delivery after OTP verification.
- Standalone invoice creation from an unverified attempt is not allowed.

## AuditLog

**Purpose**: Trace financial effects linked to delivery confirmation.

**Events**

- `CONFIRM_DELIVERY`: includes invoice creation result and Dealer balance before/after in the same transaction.
- Optional `INVOICE_AUTO_CREATE`: may be added if the codebase prefers separate audit action for financial artifact creation.

## Out Of Scope Entities

**PaymentReceipt**

- Payment collection is not mutated by this feature.
- Payment approval, rejection, receivable deduction, and Delivery Order `CLOSED` transition are handled by a separate finance/payment flow.

**Notification**

- Accountant notification is not created by this feature.
