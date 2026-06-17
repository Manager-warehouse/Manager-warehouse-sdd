# Feature: Kế toán Tiếp nhận Thông báo Lập Hóa đơn (US-WMS-10)

## 1. Context and Goal
Ngay khi đơn hàng được tài xế giao thành công và OTP được xác nhận, hệ thống tự động bắn cảnh báo đến bộ phận kế toán để lập hóa đơn bán hàng, ghi nhận công nợ đúng kỳ. Feature này chỉ tạo notification trong outbound flow; nghiệp vụ tạo invoice và cập nhật công nợ đầy đủ nằm tại [008 finance billing closing](../../008-finance-billing-closing/spec.md).

Spec 004 owns the event emission when the Delivery Order becomes `DELIVERED`. Spec 008 consumes the billing notification to create the invoice, resolve the accounting period, update dealer debt, and then move the Delivery Order to `COMPLETED`.

Realtime in-app delivery uses Spring WebSocket + STOMP. The persisted `billing_notifications` record and REST query endpoints remain the source of truth; WebSocket delivery is only the realtime push channel for authenticated Accountant sessions.

## 2. Actors
* **Kế toán viên**: Tiếp nhận billing notification cho đơn đã giao thành công và lập hóa đơn ở Spec 008.
* **Kế toán trưởng**: Không nhận billing notification thường xuyên trong Spec 004; Kế toán trưởng nhận overdue, closing, credit-limit alerts ở Spec 008.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL create an immutable billing notification event and a `BILLING_NOTIFICATION_CREATE` audit log entry whenever a delivered DO becomes ready for invoicing after OTP confirmation.
  * The system SHALL create at most one active billing notification per Delivery Order. OTP retry, duplicate `DELIVERED` transition handling, or duplicate event processing SHALL NOT create duplicate active notifications.
  * The billing notification SHALL carry `do_id`, `do_number`, `dealer_id`, dealer display data, `warehouse_id`, `delivered_at`, `total_amount_estimate`, `invoice_status`, `created_at`, and optional `read_at`.
  * The billing notification SHALL target users with the Accountant role; it SHALL NOT target Kế toán trưởng unless a separate Spec 008 alert rule applies.
  * The system SHALL persist every billing notification before pushing it via WebSocket/STOMP so disconnected users can still retrieve it through REST.
* **Event-driven:**
  * WHEN a delivery order transitions to `DELIVERED`, the system SHALL send a notification to accountants to prompt invoice creation.
  * WHEN a billing notification is created, the system SHALL push the notification through Spring WebSocket + STOMP to authenticated Accountant sessions subscribed to the billing notification topic.
  * WHEN duplicate notification creation is requested for a Delivery Order that already has an active billing notification, the system SHALL treat the operation as an idempotent no-op and return the existing notification without creating a new record.
  * WHEN a Kế toán viên marks a notification as read, the system SHALL set `read_at` without changing invoice status or accounting period.
* **State-driven:**
  * WHILE a Delivery Order is `DELIVERED` and has no invoice, the billing notification SHALL show `invoice_status = NOT_INVOICED`.
  * WHILE Spec 008 successfully creates an invoice for the Delivery Order, the Delivery Order SHALL move from `DELIVERED` to `COMPLETED`, and the billing notification SHALL be updated to `invoice_status = INVOICED` and `status = ARCHIVED`.

## 4. API Endpoints / Query Surface
* `GET /api/v1/billing-notifications` - Kế toán viên xem danh sách notification, hỗ trợ filter `status`, `invoiceStatus`, `warehouseId`, `dealerId`, `createdFrom`, `createdTo`.
* `GET /api/v1/billing-notifications/{id}` - Kế toán viên xem chi tiết notification và thông tin Delivery Order liên quan.
* `PUT /api/v1/billing-notifications/{id}/read` - Kế toán viên đánh dấu notification đã đọc.
* `GET /api/v1/delivery-orders?status=DELIVERED&invoiceStatus=NOT_INVOICED` - Query phụ để kế toán lọc các đơn đã giao thành công nhưng chưa lập invoice.

No public endpoint is required to create billing notifications manually in Sprint 1. Notification creation is triggered by the OTP-confirmed `DELIVERED` transition.

### Realtime Delivery
* Transport: Spring WebSocket + STOMP.
* Subscribers: authenticated Accountant sessions.
* Purpose: push newly created billing notifications in realtime.
* Source of truth: `billing_notifications` table and REST endpoints.
* Delivery guarantee: best-effort realtime push; missed messages are recovered by REST query.

### Payload / Response Fields
* `id`
* `doId`
* `doNumber`
* `dealerId`
* `dealerName`
* `warehouseId`
* `deliveredAt`
* `totalAmountEstimate`
* `invoiceStatus`
* `createdAt`
* `readAt`

## 5. Acceptance Criteria
* **Scenario: Notify accountant on delivery**
  * Given a delivery order transitions to `DELIVERED`
  * When the system processes the state change
  * Then the system SHALL trigger an in-app notification to the Accountant role and create audit action `BILLING_NOTIFICATION_CREATE`.

* **Scenario: Duplicate delivery event does not duplicate billing notification**
  * Given a Delivery Order already has an active billing notification
  * When the OTP confirmation is retried or the `DELIVERED` event is processed again
  * Then the system SHALL NOT create a second active billing notification for the same Delivery Order.

* **Scenario: Accountant reads notification**
  * Given an unread billing notification exists for a delivered Delivery Order
  * When Kế toán viên marks it as read
  * Then the system SHALL set `read_at` and keep invoice/accounting data unchanged.

## 6. Authorization, Tests, and Cross-Spec Boundary

* Auth: All billing notification query/read endpoints require JWT authentication.
* Authorization: Kế toán viên can view and mark billing notifications; Kế toán trưởng receives separate finance alerts in Spec 008, not regular billing notifications in Spec 004.
* Tests: Implementation MUST cover notification creation on `DELIVERED`, duplicate idempotent no-op behavior returning the existing notification, audit log creation, Accountant-only access, WebSocket/STOMP push to Accountant subscribers, read/unread behavior, and the query for `DELIVERED` + `NOT_INVOICED` Delivery Orders.
* Boundary: Spec 004 creates the notification event. Spec 008 creates the invoice, resolves `accounting_period_id`, updates dealer `current_balance`, handles `CREDIT_HOLD`, and transitions the Delivery Order to `COMPLETED`.
