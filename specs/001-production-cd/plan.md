# Implementation Plan: Production Continuous Delivery

**Branch**: `chore/cd-production-hardening` | **Date**: 2026-07-03 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-production-cd/spec.md`

## Summary

Thay quy trình build lại source trên VPS bằng quy trình build-once: GitHub Actions
kiểm tra, scan và publish backend/frontend images bất biến lên GHCR; production
chỉ pull đúng digest đã phê duyệt. Trước khi thay đổi production, workflow kiểm
tra drift và dung lượng, tạo backup PostgreSQL đã xác minh, lưu release manifest,
deploy tuần tự, chạy health/smoke tests, rồi rollback application images về
release ổn định trước nếu xác minh thất bại. Database migration chỉ tiến tới và
phải tương thích ngược.

## Technical Context

**Language/Version**: GitHub Actions YAML; POSIX shell trên Ubuntu 24.04; Java 21 / Spring Boot 3.4.5; React 18 + JavaScript

**Primary Dependencies**: GitHub Actions, GitHub Container Registry, Docker Engine + Compose v2, PostgreSQL 18 utilities, Cloudflare Tunnel

**Storage**: PostgreSQL 18 volume; backup nén ở thư mục bền vững ngoài repository/container; GHCR lưu images; GitHub artifacts lưu báo cáo ngắn hạn

**Testing**: JUnit 5 + Mockito/Spring tests; ESLint; Jest + React Testing Library sau khi bổ sung test harness; Compose validation; container health checks; smoke tests qua production domain

**Target Platform**: Một VPS Azure Ubuntu 24.04 chạy Docker Compose, truy cập qua Cloudflare Tunnel

**Project Type**: Full-stack WMS web application + deployment infrastructure

**Performance Goals**: Phát hiện unhealthy deployment <= 3 phút; rollback application <= 10 phút

**Constraints**: Không đọc/commit `.env`; không xóa migration, `/data`, `/uploads`; không downgrade Flyway tự động; không build source trên VPS; không deploy song song; không để secret vào log/artifact

**Scale/Scope**: Một production VPS, một PostgreSQL production, backend + frontend images; zero-downtime và multi-region ngoài phạm vi

**Tooling Limitation**: `pwsh` không có trong môi trường hiện tại, nên các script Speckit PowerShell được thay bằng kiểm tra đường dẫn và tạo artifact thủ công tương đương.

## Constitution Check

*GATE: Passed before Phase 0 and re-checked after Phase 1 design.*

- [x] Layered architecture preserved: no application-layer changes planned.
- [x] Write endpoints use request DTOs with Jakarta Validation: no endpoint changes.
- [x] Service business rules, transactions, authorization, and audit logging remain unchanged.
- [x] No application database access or raw SQL is introduced.
- [x] Inventory invariants remain covered by backend regression tests before release.
- [x] QC/quarantine/transfer/accounting state rules are not changed.
- [x] Business audit schema is unchanged; release evidence is operational and append-only.
- [x] OpenAPI/Swagger impact: none; health/smoke checks use approved existing surfaces.
- [x] Flyway impact identified: no migration in this feature; pipeline validates and never rewrites migrations.
- [x] Test strategy covers gate failure, deploy failure, health failure, rollback, backup failure, and secret leakage.

## Domain Impact

**Actors/Roles**: Developer, Reviewer, Release Approver, Operator; no WMS JWT role or warehouse-scope changes

**State Changes**: Release `REQUESTED -> VERIFIED -> APPROVED -> DEPLOYING -> HEALTHY`; failure branches to `BLOCKED`, `ROLLED_BACK`, or `INCIDENT`

**Inventory Impact**: None; release gates continue running invariant regression tests

**Audit Actions**: Operational release evidence includes requester, approver, source SHA, image digests, backup reference, gate results, timestamps and final state; no business audit mutation

**Security/Authorization**: GitHub production environment approval, least-privilege workflow permissions, pinned SSH fingerprint, scoped registry access; no JWT changes

**Accounting Impact**: None; backup and forward-only migration policy protect existing records

## Data Model / Migration Impact

- Entities/tables touched: none
- New/changed columns or constraints: none
- Flyway plan: no migration; existing applied migrations remain immutable
- Backfill/seed data: none
- Operational records: JSON release manifest and backup metadata outside the application database

## API / Contract Impact

- Endpoints added/changed: none
- Request DTOs: none
- Response DTOs: none
- Error codes/statuses: none
- OpenAPI path/schema updates: none
- Operational contracts: `contracts/release-manifest.schema.json` and `contracts/deployment-contract.md`

## Test Strategy

- Backend gate: `mvn -B test` with existing PostgreSQL service and uploaded Surefire reports.
- Frontend gate: `npm ci`, `npm run lint`, `npm test --if-present`, `npm run build`; add a real Jest harness as a separate prerequisite if no tests exist.
- Image gate: Compose config validation, Buildx build, vulnerability scan with HIGH/CRITICAL policy, publish by source SHA, record digest.
- Backup gate: validate destination, disk headroom, non-empty compressed dump, checksum and metadata; restore drill is an operational readiness test, not every deploy.
- Deployment tests: drift rejection, missing approval, missing/invalid backup, concurrent deploy serialization, exact digest verification.
- Post-deploy tests: container health, frontend public HTTPS, backend/API reachability through Nginx, bounded read-only authenticated smoke check.
- Rollback tests: intentionally unhealthy release in staging/test host, restore previous digests, re-run health/smoke checks, ensure no Flyway downgrade.
- Security tests: secret scanning of workflow logs/artifacts and least-privilege permission review.

## Project Structure

### Documentation

```text
specs/001-production-cd/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── deployment-contract.md
│   └── release-manifest.schema.json
├── checklists/
│   ├── requirements.md
│   └── cd-readiness.md
└── tasks.md
```

### Source Code

```text
.github/workflows/
└── deploy.yml

scripts/deploy/
├── backup-database.sh
├── deploy-release.sh
├── smoke-test.sh
└── rollback-release.sh

compose.prod.yaml
DEPLOY_GUIDE.md
frontend/package.json
frontend/src/**/*.test.jsx
```

**Structure Decision**: Giữ một workflow production nhưng tách logic có thể kiểm
thử sang `scripts/deploy/`. Compose production nhận image reference bất biến từ
environment thay vì build context. Release manifest là ranh giới dữ liệu giữa
workflow và scripts, giúp deploy/rollback cùng dùng một nguồn sự thật.

## Delivery Phases

### Phase A - Quality gates

1. Bổ sung frontend test harness tối thiểu và chạy lint trong CI.
2. Tách verification khỏi deployment; bảo toàn Surefire và scan reports.
3. Cấu hình permissions tối thiểu và production environment approval.

### Phase B - Immutable artifacts

1. Build backend/frontend một lần bằng Buildx.
2. Scan images trước publish và chặn theo policy đã định.
3. Push GHCR tags theo commit SHA, capture immutable digests và tạo release manifest.

### Phase C - Safe production deployment

1. Kiểm tra production drift, disk capacity, Compose và secret prerequisites.
2. Tạo backup có checksum và metadata trước mọi deploy có khả năng chạy Flyway.
3. Pull images theo digest, lưu previous manifest, deploy và xác minh exact digest.

### Phase D - Verification and rollback

1. Chạy health checks trong giới hạn 3 phút và smoke tests qua Cloudflare.
2. Nếu lỗi, thu thập diagnostics và rollback application theo previous manifest.
3. Nếu rollback không tương thích hoặc thất bại, dừng ở trạng thái incident; không restore DB tự động.

### Phase E - Operational handoff

1. Cập nhật deploy guide, secret/variable inventory và runbook.
2. Chạy rehearsal trên môi trường không phải production.
3. Thực hiện first production release có giám sát và review bằng chứng sau deploy.

## Complexity Tracking

Không có vi phạm constitution. Việc thêm registry và release manifest là cần
thiết để bảo đảm artifact đã kiểm tra chính là artifact được deploy; chúng không
mở rộng domain application.
