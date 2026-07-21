# Research: Production Continuous Delivery

## Decision 1: Build once and deploy immutable image digests

**Decision**: Build backend/frontend trong GitHub Actions, publish lên GHCR theo
commit SHA và deploy bằng digest.

**Rationale**: Loại bỏ khác biệt giữa image được CI kiểm tra và image VPS tự build;
digest cho phép đối chiếu chính xác và rollback có thể lặp lại.

**Alternatives considered**:

- Build lại trên VPS: đơn giản nhưng không bảo đảm artifact identity và tăng tải/rủi ro production.
- Chuyển tar image qua SSH: không cần registry nhưng quản lý retention, integrity và rollback kém hơn.

## Decision 2: Recreate deployment with bounded downtime

**Decision**: Giữ một instance mỗi service và cho phép khoảng gián đoạn ngắn,
thay vì blue/green trong Sprint 1.

**Rationale**: Phù hợp giả định một VPS, giảm phức tạp routing/database migration;
health và rollback targets vẫn đo được.

**Alternatives considered**:

- Blue/green: giảm downtime nhưng cần gấp đôi tài nguyên, routing switch và migration compatibility nghiêm ngặt hơn.
- Rolling replicas: không phù hợp Compose single-host hiện tại nếu chưa có orchestration/load balancing.

## Decision 3: Backup gate plus forward-only migrations

**Decision**: Tạo backup có checksum trước deploy có khả năng chạy migration;
migrations phải tương thích ngược và không bị downgrade tự động.

**Rationale**: Rollback schema tự động nguy hiểm với dữ liệu mới. Expand/contract
cho phép application rollback trong khi bảo toàn lịch sử dữ liệu.

**Alternatives considered**:

- Flyway undo/downgrade tự động: không phù hợp quy tắc migration bất biến và có nguy cơ mất dữ liệu.
- Restore DB tự động: có thể xóa giao dịch phát sinh sau backup, chỉ phù hợp incident có phê duyệt.

## Decision 4: Production environment approval

**Decision**: Dùng một production environment có required reviewer; manual và
main-triggered releases dùng cùng pipeline và gates.

**Rationale**: Tách maker/checker, giữ trace approval và không tạo đường bypass thủ công.

**Alternatives considered**:

- Auto-deploy ngay khi merge: nhanh nhưng thiếu checker gate cho hệ thống WMS đang phát triển.
- SSH thủ công: khó truy vết và dễ bỏ qua kiểm tra.

## Decision 5: Shell scripts as testable deployment units

**Decision**: Workflow điều phối; backup, deploy, smoke và rollback nằm trong các
script nhỏ, strict mode, input validation và log redaction.

**Rationale**: YAML ngắn hơn, logic có thể chạy/review độc lập, giảm duplicate giữa deploy và rehearsal.

**Alternatives considered**:

- Toàn bộ logic inline YAML: ít file nhưng khó test, rollback dài và dễ lệch.
- Deployment platform mới: vượt scope Sprint 1 và tăng vận hành.

## Tooling Constraint

Speckit PowerShell scripts không chạy được vì môi trường thiếu `pwsh`. Feature
path được lấy từ `.specify/feature.json`; artifacts được tạo và kiểm tra thủ công
theo cùng templates và gates.
