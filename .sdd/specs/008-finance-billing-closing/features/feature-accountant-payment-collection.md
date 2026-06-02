# Feature: Kế toán Ghi nhận Thanh toán & Quản lý Công nợ (US-WMS-15)

## 1. Context and Goal
Kế toán ghi nhận thanh toán thu tiền từ Đại lý, cấn trừ hóa đơn và giảm dư nợ Đại lý. Hệ thống tự động mở khóa tín dụng (chuyển sang `ACTIVE`) khi dư nợ giảm dưới 80% hạn mức (buffer 20%). Ngoài ra, Daily Job quét các hóa đơn quá hạn >30 ngày để khóa công nợ.

## 2. Actors
* **Kế toán viên (Maker)**: Lập phiếu thu cấn trừ công nợ.
* **Hệ thống (Daily Job)**: Tự động chạy quét hóa đơn quá hạn.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL always update `current_balance` with every payment receipt.
* **Event-driven:**
  * WHEN a payment is recorded, the system SHALL:
    * Decrease dealer's `current_balance` by payment amount.
    * Mark matched invoices as `PAID` (or `PARTIALLY_PAID`).
    * IF `current_balance < credit_limit × 0.8`: auto-set dealer status to `ACTIVE`.
  * WHEN the daily job runs, the system SHALL scan all unpaid invoices past their due date by > 30 days and auto-set the dealer status to `CREDIT_HOLD`.

## 4. API Endpoints
* `POST /api/v1/payments` - Lập phiếu thu, cấn trừ hóa đơn.
* `GET /api/v1/payments` - Danh sách phiếu thu.

## 5. Acceptance Criteria

**Scenario 1: Payment recorded without reaching unlock threshold**
* Given a dealer with `credit_limit = 500M`, status `CREDIT_HOLD`, and `current_balance = 600M`
* When a payment of `200M` is recorded (new balance = 400M, which is equal to 500M * 0.8)
* Then the dealer status SHALL remain `CREDIT_HOLD` and system warns they need to pay more to unlock.

**Scenario 2: Payment recorded and credit status unlocked**
* Given a dealer with `credit_limit = 500M`, status `CREDIT_HOLD`, and `current_balance = 600M`
* When a payment of `201M` is recorded (new balance = 399M, which is < 400M)
* Then the dealer status SHALL be auto-unlocked to `ACTIVE`.
