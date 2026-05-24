# CONSTITUTION.md — Project Law
# Warehouse Management System (WMS)
# Ratified: 2026-05-24 | Team: WMS Development Team | Version: 1.0

## RULE: Any change to this document requires **unanimous team approval** and version bump.

---

## ARTICLE 1 — TECH STACK (immutable)
Runtime: Java 21.0.10
Framework: Spring Boot 3.4.5
Database: PostgreSQL 18 — NO NoSQL without team vote
Frontend: React 18 + TypeScript — NO class components
Styling: Tailwind CSS 3.x — NO CSS-in-JS, NO custom CSS unless Tailwind insufficient
ORM: Spring Data JPA / Hibernate — NO raw SQL in application code
Auth: JWT + bcrypt (min cost 12)
Package manager: npm (frontend), Maven (backend)
Testing: JUnit 5 + Mockito (backend), Jest (frontend)

---

## ARTICLE 2 — CODING STANDARDS
Language: TypeScript strict mode (noImplicitAny: true), Java with strict null checks
Formatter: Prettier (auto-format on save — no config debates)
Linter: ESLint + Airbnb config (0 warnings allowed in CI)
Max function length: 40 lines (refactor if longer)
Max file length: 300 lines (split if longer)
Comments: explain WHY not WHAT. Remove TODO before merge.
Logging: SLF4J only — NO System.out/console.log in production
Audit: All warehouse operations MUST log (user, action, timestamp, before/after)

---

## ARTICLE 3 — SECURITY POLICIES (non-negotiable)
- Passwords: bcrypt with min cost 12 — NEVER plain text or MD5
- API keys: environment variables ONLY — never in source code
- SQL: JPA/Hibernate ORM ONLY — zero tolerance for string concatenation in queries
- File uploads: validate type + size (max 10MB) + scan for malware
- Input validation: Jakarta Validation annotations on every request body
- CORS: whitelist only — no wildcard (*) in production
- HTTPS: required for all production endpoints
- Secrets: NEVER store passwords/API keys in .env files committed to git

---

## ARTICLE 4 — BUSINESS RULES (enforced by system)
- Inventory NEVER goes negative — system blocks any transaction that would cause it
- Batch has ONE grade only (Grade A/B/C) — no mixed-grade batches
- FEFO (First Expired First Out) for products WITH expiry date
- FIFO (First In First Out) for products WITHOUT expiry date
- Quarantine Zone: QC-failed goods isolated, NOT in available inventory
- In-Transit Location: virtual warehouse for transfers in progress
- All warehouse events (receipt/issue/transfer/adjustment) MUST notify Accounting
- Sale Orders auto-create warehouse preparation tasks

---

## ARTICLE 5 — GIT WORKFLOW
Main branch: protected — no direct push
Branch naming: feat/ | fix/ | spec/ | chore/
Commit format: [type]([scope]): [description] (max 72 chars)
PR rules: min 1 approval from code reviewer before merge
PR size: max 400 lines changed (split larger PRs)
Merge strategy: Squash and merge for feature branches

---

## ARTICLE 6 — TESTING REQUIREMENTS
Minimum coverage: 80% for all new code
Required: unit tests for all service/business logic classes
Required: integration tests for all API endpoints (happy + error paths)
E2E tests: required for critical flows (receipt, issue, transfer, delivery)
No merge if existing tests break
FEFO/FIFO logic must have dedicated test cases

---

## ARTICLE 7 — REVIEW PROCESS
Code review: async via PR, min 1 approval
Spec review: Monday Spec Sync — no code before spec approval
Architecture changes: require Constitution amendment vote
Emergency hotfix: allowed with 1 approval + post-mortem required
Performance review: monthly, focus on query optimization

---

## ARTICLE 8 — API DESIGN STANDARDS
RESTful endpoints with proper HTTP methods
Endpoints: /api/[resource-name] (kebab-case)
Response format: `{ data, message, code, timestamp }`
Error format: `{ error, code, message, timestamp }`
Pagination: cursor-based for large datasets
Versioning: /api/v1/ prefix

---

## ARTICLE 9 — DEPLOYMENT & OPERATIONS
Environment: Development, Staging, Production
CI/CD: GitHub Actions / Jenkins
Docker: required for local development
Backup: daily, retention 30 days
Monitoring: application metrics + business KPIs
On-call: rotation among senior developers

---

## ARTICLE 10 — AI AGENT RULES
- Read AGENTS.md before starting any session
- Read CLAUDE.md for project memory and patterns
- Review plan BEFORE approving execution
- Human-Led Refactoring after every 5 agent tasks
- All agent-generated code must pass Pre-Commit Checklist
- Never approve agent output you cannot explain to another team member
- Use Speckit CLI commands for spec-driven workflow:
  - `/speckit-specify` — tạo/cập nhật spec từ mô tả ngôn ngữ tự nhiên
  - `/speckit-plan` — sinh design artifacts (schema, API, flow)
  - `/speckit-tasks` — tạo task list có thể thực thi từ design
  - `/speckit-implement` — thực thi các task trong tasks.md
  - **Cài đặt**: Speckit là skills trong `.agents/skills/` — sử dụng qua AI agent (Antigravity/Claude Code)

---

## ARTICLE 11 — PRE-COMMIT CHECKLIST
- [ ] No hardcoded secrets (API keys, passwords)
- [ ] Unit tests pass (mvn test / npm test)
- [ ] Linting clean (mvn checkstyle / npm run lint)
- [ ] Type checking passed (npm run typecheck)
- [ ] Audit logs included for warehouse operations
- [ ] No TODO comments left in code
- [ ] FEFO/FIFO logic tested for batch operations

---

## ARTICLE 12 — WAREHOUSE-SPECIFIC RULES
- 3 physical warehouses: Hải Phòng (HP), Hà Nội (HN), Hồ Chí Minh (HCM)
- 1 virtual warehouse: In-Transit (for transfers)
- 1 quarantine zone per warehouse (for QC failures)
- Role-based access: Admin, Warehouse Manager, Warehouse Clerk, Accountant (read-only), Sales
- Warehouse managers can ONLY access assigned warehouse(s)
- All transfers create In-Transit inventory until destination confirms receipt

---

## ARTICLE 13 — INTEGRATION RULES
- Accounting: auto-send warehouse events (receipt cost, issue revenue, COGS)
- HRM: send staff productivity data (units processed, shifts worked)
- Sales: receive Sale Orders, send delivery status updates
- NO direct database access between systems — API/message queue only

---

## ARTICLE 14 — CHANGELOG & VERSIONING

**Format**: `[DATE] v[MAJOR].[MINOR] [Article X v{n}] — [Mô tả thay đổi]`
- `MAJOR` tăng khi có thay đổi phá vỡ (breaking change) quy trình
- `MINOR` tăng khi bổ sung hoặc sửa nhỏ trong 1 Article

| Date | Version | Article | Mô tả |
|------|---------|---------|--------|
| 2026-05-24 | v1.0 | All | Initial Constitution ratified for WMS project |
