# Feature: Kế toán trưởng Xem Báo cáo Công nợ Phân kỳ (US-WMS-16)

## 1. Context and Goal
Kế toán trưởng theo dõi báo cáo công nợ phân kỳ (Aging Report) chia thành các mốc: trong hạn, quá hạn 1-30 ngày, quá hạn 31-60 ngày, quá hạn >60 ngày để đánh giá rủi ro tín dụng của hệ thống đại lý.

## 2. Actors
* **Kế toán trưởng (Checker)**: Xem và xuất báo cáo phân kỳ công nợ.

## 3. Functional Requirements (EARS)
* **Optional:**
  * WHERE an invoice is overdue by >60 days, the system SHALL flag it with a High Risk warning on the aging report.

## 4. API Endpoints
* `GET /api/v1/credit/aging-report` - Xuất báo cáo công nợ phân kỳ của các đại lý.

## 5. Acceptance Criteria
* **Scenario: Generate aging report**
  * Given a dealer has 1 invoice unpaid in 45 days (overdue 31-60) and 1 invoice in-term
  * When Kế toán trưởng requests the aging report
  * Then the system SHALL group those amounts into their respective aging buckets correctly.
