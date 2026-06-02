# Feature: Nhật ký Hoạt động Hệ thống (Audit Log)

## 1. Context and Goal
Để đảm bảo tính minh bạch, kiểm soát rủi ro, và ghi nhận dấu vết giao dịch, hệ thống tự động ghi nhật ký (Audit Log) cho mọi hành động tạo, sửa, phê duyệt, hủy hoặc xóa trên các thực thể nghiệp vụ cốt lõi.

## 2. Actors
* **System**: Hệ thống tự động ghi log khi thực hiện các giao dịch.
* **System Admin / CEO**: Xem và tra cứu lịch sử nhật ký.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL always record an audit log entry for every CREATE, UPDATE, DELETE, APPROVE, REJECT, or CANCEL operation on any business entity.
* **Event-driven:**
  * WHEN a user's role or warehouse assignment changes, the system SHALL log the change with before/after state in the audit log.

## 4. API Endpoints
* `GET /api/v1/audit-logs` - Tra cứu nhật ký hệ thống (hỗ trợ lọc theo actor, entity, action, time).

## 5. Acceptance Criteria

**Scenario: Audit Log Creation**
* Given a valid authenticated session
* When the user creates or modifies any warehouse operation (receipt, issue, transfer, config changes)
* Then the system SHALL create an audit log entry with actor_id, actor_role, action, timestamp, before_state, and after_state.
