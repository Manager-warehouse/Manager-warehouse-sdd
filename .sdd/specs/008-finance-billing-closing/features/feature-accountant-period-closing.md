# Feature: Kế toán trưởng Chốt sổ Kế toán & Khóa Kỳ (US-WMS-17)

## 1. Context and Goal
Kế toán trưởng thực hiện chốt sổ định kỳ hàng tháng để khóa cứng toàn bộ dữ liệu lịch sử. Bất kỳ chứng từ phát sinh trễ hạn nào phải được hạch toán vào kỳ hiện tại, và sai sót trong kỳ cũ phải điều chỉnh bằng phiếu điều chỉnh ngược trong kỳ hiện hành.

## 2. Actors
* **Kế toán trưởng (Checker)**: Người duy nhất có thẩm quyền thực hiện chốt sổ tháng.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN Kế toán trưởng requests to close a period, the system SHALL automatically verify that there are no pending/unapproved receipts, delivery orders, or invoices in that period.
  * WHEN the current calendar day reaches or exceeds the `MONTHLY_CLOSING_DAY` system parameter, the system SHALL notify the Kế toán trưởng to review and close the accounting period.
* **State-driven:**
  * WHILE an accounting period status is `CLOSED`, the system SHALL reject any CREATE, UPDATE, or DELETE transactions (receipts, delivery orders, transfers, adjustments, stocktakes, invoices, payments) with a `transaction_date` falling within that closed period.

## 4. API Endpoints
* `PUT /api/v1/accounting/periods/{period}/close` - Đóng/Chốt sổ kỳ kế toán.

## 5. Acceptance Criteria
* **Scenario: Prevent edits to closed periods**
  * Given accounting period `2026-04` has status `CLOSED`
  * When a user attempts to update a DO dated `2026-04-15`
  * Then the system SHALL reject the operation with a `PERIOD_CLOSED` (422) error.
