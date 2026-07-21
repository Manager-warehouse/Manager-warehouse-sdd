# Data Model: Production Continuous Delivery

Feature không thay đổi entity hoặc table của WMS. Các cấu trúc dưới đây là dữ
liệu vận hành dạng file/artifact, không nằm trong PostgreSQL application schema.

## Release Manifest

| Field | Required | Rule |
|-------|----------|------|
| `releaseId` | Yes | Duy nhất, bất biến |
| `sourceSha` | Yes | Full commit SHA |
| `createdAt` | Yes | UTC timestamp |
| `requestedBy` | Yes | Actor yêu cầu release, không chứa credential |
| `approvedBy` | Yes | Release Approver khác requester |
| `backendImage` | Yes | Registry reference pinned by digest |
| `frontendImage` | Yes | Registry reference pinned by digest |
| `previousReleaseId` | No | Release ổn định trước, nếu tồn tại |
| `backupId` | Conditional | Bắt buộc trước production mutation |
| `gateResults` | Yes | Kết quả hữu hạn cho từng mandatory gate |
| `approvedAt` | Yes | UTC timestamp trước khi deploy |
| `verificationStartedAt` | Conditional | Bắt buộc từ trạng thái `DEPLOYING` |
| `verificationCompletedAt` | Conditional | Bắt buộc ở terminal state |
| `status` | Yes | Giá trị trong state machine |

## Backup Metadata

| Field | Required | Rule |
|-------|----------|------|
| `backupId` | Yes | Duy nhất theo timestamp + source SHA |
| `createdAt` | Yes | UTC timestamp |
| `database` | Yes | Tên logical database, không chứa credential |
| `path` | Yes | Nằm ngoài repository và container lifecycle |
| `sizeBytes` | Yes | Lớn hơn 0 |
| `sha256` | Yes | Checksum 64 ký tự hex |
| `sourceReleaseId` | Yes | Release đang chạy lúc backup |
| `retentionUntil` | Yes | Tối thiểu 14 ngày sau `createdAt` |

## Release State Transitions

```text
REQUESTED -> VERIFIED -> APPROVED -> DEPLOYING -> HEALTHY
     |           |           |           |
     +--------> BLOCKED <-----+           +-> ROLLED_BACK
                                             |
                                             +-> INCIDENT
```

- `HEALTHY` chỉ được đặt sau internal health và public smoke tests.
- `ROLLED_BACK` chỉ được đặt khi previous release vượt lại các xác minh.
- `INCIDENT` là terminal automation state; cần can thiệp có phê duyệt.
- Không được chuyển trực tiếp từ `REQUESTED` sang `DEPLOYING`.

## Validation Rules

- Image reference production phải có digest; mutable tag đơn thuần không hợp lệ.
- Manifest không chứa secret, `.env` content hoặc dữ liệu nghiệp vụ.
- `approvedBy` phải khác `requestedBy` để giữ maker/checker separation.
- `gateResults` phải có backend tests, frontend lint/tests/build, production config, image build, vulnerability policy và manifest validation.
- Backup metadata không đủ điều kiện nếu file thiếu, size bằng 0 hoặc checksum không khớp.
- Previous release phải là release từng đạt `HEALTHY`.
