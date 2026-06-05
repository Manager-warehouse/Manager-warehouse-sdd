# Data Model: Accountant Partner & Credit Limit Management

## Dealer

Represents a customer/dealer account used by delivery, sales, finance, and credit workflows.

**Fields**

- `id`: primary key
- `code`: unique dealer code
- `name`: required dealer display/legal name
- `phone`: optional contact phone
- `defaultDeliveryAddress`: optional default delivery address mapped to `default_delivery_address`
- `region`: optional dealer region
- `credit_limit`: positive decimal amount; must be greater than `current_balance`
- `current_balance`: non-negative decimal balance from finance transactions
- `payment_term_days`: allowed values `30` or `60`
- `credit_status`: `ACTIVE` or `CREDIT_HOLD`; defaults to `ACTIVE`
- `is_active`: soft-delete flag; defaults to `true`
- `created_at`, `updated_at`: timestamps

**Validation Rules**

- `code` must be unique.
- `credit_limit > 0`.
- `credit_limit > current_balance`.
- Dealer create initializes `payment_term_days` from `DEFAULT_PAYMENT_TERM_DAYS`, `credit_limit` from `DEFAULT_CREDIT_LIMIT`, `current_balance = 0`, `credit_status = ACTIVE`, and `is_active = true`.
- Dealer profile create/update DTOs do not expose credit-control fields to `ACCOUNTANT`.
- New dealer transactions are allowed only when `is_active = true`, `credit_status = ACTIVE`, and `current_balance + transaction_amount <= credit_limit`.
- When a new transaction would exceed the limit, reject only that transaction, set `credit_status = CREDIT_HOLD`, and return a credit-limit reason.
- Inactive dealers block all new dealer operational activity regardless of `credit_status`.

## Supplier

Represents a procurement/inbound partner.

**Fields**

- `id`: primary key
- `code`: unique supplier code
- `company_name`: required display/legal name
- `tax_code`: optional, non-unique
- `phone`: optional contact phone
- `address`: optional address
- `contact_person`: optional primary contact
- `is_active`: soft-delete flag; defaults to `true`
- `created_at`, `updated_at`: timestamps

**Validation Rules**

- `code` must be unique.
- `tax_code` is optional and not unique.
- Supplier management keeps historical purchase order, receipt, QC, debit-note, and audit records readable after deactivation.
- Reactivation restores the supplier profile to active status and does not alter historical records.

## Audit Log

Existing audit logging must be used for all successful partner mutations.

**Required audit fields**

- `actor_id`
- `actor_role`
- `action`
- `entity_type`
- `entity_id`
- `timestamp`
- `changed_fields` with before/after values

**Actions**

- Dealer: create, update profile, update credit limit, update payment term, update credit status, deactivate, reactivate, automatic credit hold.
- Supplier: create, update profile, deactivate, reactivate.

## Request DTOs

**DealerCreateRequest / DealerUpdateRequest**

- Existing dealer profile fields: `code`, `name`, `phone`, `defaultDeliveryAddress`, `region`.
- Profile updates by `ACCOUNTANT`.
- Must not expose unrestricted credit fields in normal profile update when only `ACCOUNTANT_MANAGER` may change them.
- Create service fills `paymentTermDays`, `creditLimit`, `currentBalance`, `creditStatus`, and `isActive` from defaults/system rules rather than request body.

**DealerCreditLimitUpdateRequest**

- `credit_limit`: required positive decimal.
- Requires `ACCOUNTANT_MANAGER`.

**DealerPaymentTermUpdateRequest**

- `payment_term_days`: required integer, allowed `30` or `60`.
- Requires `ACCOUNTANT_MANAGER`.

**DealerCreditStatusUpdateRequest**

- `credit_status`: required enum `ACTIVE` or `CREDIT_HOLD`.
- Requires `ACCOUNTANT_MANAGER`.

**SupplierCreateRequest / SupplierUpdateRequest**

- `code`, `company_name`, `tax_code`, `phone`, `address`, `contact_person`.
- `code` is unique; `tax_code` optional and non-unique.

## Response DTOs

**DealerResponse**

- Public dealer profile fields, credit fields, `is_active`, created/updated timestamps.

**SupplierResponse**

- Public supplier profile fields, `is_active`, created/updated timestamps.

## State Transitions

```text
Dealer.credit_status:
ACTIVE -> CREDIT_HOLD
  - Automatic when a new transaction would exceed credit limit.
  - Manual by ACCOUNTANT_MANAGER.
CREDIT_HOLD -> ACTIVE
  - Manual by ACCOUNTANT_MANAGER.
```

```text
Partner.is_active:
true -> false
  - Soft delete by ACCOUNTANT after business guards pass.
false -> true
  - Reactivation by ACCOUNTANT.
```

## Downstream Guards

- Delivery/order creation must call dealer eligibility before accepting a dealer reference.
- Dealer transaction creation must call credit check before creating the transaction.
- Supplier received-order views read existing `receipts` records and do not create inbound receipt records.
