# Dealer & Credit API — /api/v1/dealers, /api/v1/credit

## GET /api/v1/dealers
**Query**: ?page=0&size=20&status=ACTIVE&search=keyword
**Response 200**: Paginated dealer list

## POST /api/v1/dealers
**Request**: `{ "code", "name", "credit_limit", "payment_terms" }`
**Response 201**: Dealer object
**Note**: Only ACCOUNTANT_MANAGER can set credit_limit

## GET /api/v1/dealers/{id}
**Response 200**: Dealer with balance, credit status, recent invoices

## PUT /api/v1/dealers/{id}
**Request**: `{ "credit_limit", "payment_terms" }`
**Response 200**: Updated dealer

## GET /api/v1/dealers/{id}/credit-history
**Response 200**: Credit status change history

## GET /api/v1/credit/aging-report
**Response 200**: Aging report (current, 1-30, 31-60, 60+ days overdue)

## GET /api/v1/invoices
**Query**: ?dealerId=X&status=PAID&page=0&size=20
**Response 200**: Paginated invoice list

## POST /api/v1/invoices
**Request**: `{ "dealer_id", "delivery_order_id", "total_amount" }`
**Response 201**: Invoice, dealer balance increased

## POST /api/v1/payments
**Request**: `{ "dealer_id", "invoice_ids": [...], "amount", "reference" }`
**Response 201**: Payment recorded, invoices marked PAID, balance reduced, credit check re-evaluated
