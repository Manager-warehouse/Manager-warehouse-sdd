# CLAUDE.md — Warehouse Management System (WMS) v1.0

## Hệ Thống Quản Lý Kho cho doanh nghiệp thương mại

---

## TL;DR (Đọc trước — 60 giây)

> **Đây là hệ thống quản lý kho hàng (Warehouse Management System)**
>
> **Backend**: Spring Boot 3.4.5 + Java 21 + PostgreSQL 18 + JPA/Hibernate
> **Frontend**: React 18 + JavaScript + Tailwind CSS 3.x
> **Auth**: JWT + bcrypt (cost factor 12)
> **Scope**: WMS, Kế toán nội bộ kho, Điều phối vận tải nội bộ, Công nợ Đại lý (chạy chung DB & hệ thống, không dùng Message Queue)
>
> **3 warehouses**: Hải Phòng, Hà Nội, Hồ Chí Minh (chỉ dùng xe nội bộ Phúc Anh)
> **Scale**: 1000+ products, 50+ dealers, 1000+ transactions/month

### Đọc trước

1. `AGENTS.md` → Project context đầy đủ (Tech stack, forbidden patterns, domain model)
2. `README.md` → User Stories, requirements, key entities
3. File này → Workflow, patterns, và conventions
4. 'CONSTITUTION.md'

---

## KIẾN TRÚC HỆ THỐNG

### Sơ đồ tổng quan

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         FRONTEND (React 18)                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐ │
│  │Dashboard │  │Inventory │  │Receipt/  │  │Transfer  │  │Delivery  │ │
│  │          │  │          │  │Issue     │  │          │  │          │ │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘ │
└───────┼─────────────┼─────────────┼─────────────┼─────────────┼───────┘
        │             │             │             │             │
        ▼             ▼             ▼             ▼             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    BACKEND (Spring Boot 3.4.5)                          │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    REST API Layer (@RestController)              │   │
│  │  /api/v1/warehouse-stock  /api/v1/batch-management  /api/v1/receipt  │   │
│  │  /api/v1/issue  /api/v1/transfer  /api/v1/delivery  /api/v1/sale-order│   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                              │                                          │
│                              ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                 Service Layer (@Service)                        │   │
│  │  WarehouseService  BatchService  InventoryService  ReceiptService│   │
│  │  IssueService  TransferService  DeliveryService  SaleOrderService│   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                              │                                          │
│                              ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │              Repository Layer (Spring Data JPA)                  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                              │
        ┌──────────────────────┴──────────────────────┐
        ▼                                             ▼
┌─────────────────┐                          ┌─────────────────┐
│   PostgreSQL    │                          │   File Storage  │
│   (Primary DB)  │                          │   (/uploads)    │
└─────────────────┘                          └─────────────────┘
```

### Layer Architecture (Backend)

```
┌─────────────────────────────────────────┐
│          Controller Layer               │  @RestController
│   - Input validation                    │  - Handle HTTP requests
│   - Response formatting                 │  - Return DTOs
│   - HTTP status codes                   │
└─────────────────┬─────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│           Service Layer                  │  @Service
│   - Business logic                      │  - Transaction management
│   - FEFO/FIFO selection                 │  - Audit logging
│   - Authorization checks                │
└─────────────────┬─────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│          Repository Layer                │  @Repository
│   - JPA/Hibernate queries               │  - Never raw SQL
│   - Entity mapping                      │
└─────────────────┬─────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│           Entity Layer                  │  @Entity
│   - Database table mapping              │  - JPA annotations
│   - Relationships                       │
└─────────────────────────────────────────┘
```

### Repository Structure

```
Manager-warehouse-sdd/
├── .git/                        # Git metadata
├── AGENTS.md                    # Agent policy, tech stack, forbidden patterns
├── CLAUDE.md                    # Kiến trúc, workflow, conventions (file này)
├── Kiến trúc phân tầng các Actors.md  # 10 Actors, nghiệp vụ, quy trình
├── README.md                    # User Stories, requirements, key entities
├── DESIGN.md                    # UI design tokens (Apple design system)
├── Userstory.md                 # User Stories bổ sung
├── backend/                     # Spring Boot 3.4.5 + Java 21
├── frontend/                    # React 18 + Tailwind CSS
├── specs/                       # Feature specifications (SDD)
│   └── 001-featurename-us-wh/
│       └── spec.md
└── test/                        # Test plans and guidance
```

---

## QUYẾT ĐỊNH KIẾN TRÚC QUAN TRỌNG (ADR)

### ADR-001: Spring Boot 3.4.5 + Java 21 thay vì Node.js

**Quyết định**: Chọn Java ecosystem cho backend
**Lý do**: Team có kinh nghiệm Java enterprise, type safety mạnh, ecosystem ổn định
**Trade-off**: Verbose hơn Node.js, compile time dài hơn
**Status**: ✅ Approved

### ADR-002: Spring Data JPA/Hibernate cho ORM

**Quyết định**: Không dùng raw SQL, luôn dùng JPA/Hibernate
**Lý do**: Type safety, relational integrity, migration support qua Flyway
**Trade-off**: Complex queries có thể chậm hơn raw SQL
**Status**: ✅ Approved

### ADR-003: JWT + bcrypt (cost factor 12) cho Auth

**Quyết định**: Stateless authentication với hashed passwords
**Lý do**: Scalable, stateless, industry standard
**Trade-off**: Token management, refresh mechanism needed
**Status**: ✅ Approved

### ADR-004: Tách Kho ảo "In-Transit Location"

**Quyết định**: Virtual warehouse cho hàng đang di chuyển giữa các kho
**Lý do**: Track inventory during transfer, không mất visibility
**Trade-off**: Complex queries khi join với real warehouses
**Status**: ✅ Approved

### ADR-005: Quarantine Zone cho QC-failed goods

**Quyết định**: Tách zone riêng cho hàng không đạt QC
**Lý do**: Không tính vào available inventory, track rejects separately
**Trade-off**: Thêm complexity trong inventory calculations
**Status**: ✅ Approved

### ADR-006: Đồng bộ dữ liệu Kế toán nội bộ trực tiếp

**Quyết định**: Xây dựng phân hệ Kế toán nội bộ kho và Công nợ Đại lý chạy chung trên cùng một Backend/Database, không dùng Message Queue (Kafka/RabbitMQ).
**Lý do**: Hệ thống Phúc Anh quản lý tập trung, đơn giản hóa kiến trúc, đảm bảo tính nhất quán dữ liệu (ACID transactions) trực tiếp khi thực hiện các giao dịch nhập/xuất kho mà không cần cơ chế sync phức tạp và giảm thiểu overhead vận hành.
**Trade-off**: Tải trọng DB tăng nhẹ khi xử lý giao dịch đồng thời lớn.
**Status**: ✅ Approved

---

## NHỮNG GÌ ĐÃ KHÔNG HOẠT ĐỘNG (Lessons Learned)

### LESSON-001: Never allow negative inventory

**Biến cố**: [TBD] Đã từng để xảy ra tồn kho âm → conflict inventory
**Giải pháp**: Luôn check stock TRƯỚC khi issue, dùng database constraint
**Áp dụng**: Tất cả issue operations phải validate quantity

### LESSON-002: Batch tied to ONE grade only

**Biến cố**: [TBD] Mixed grade trong batch → FEFO selection confusion
**Giải pháp**: Mỗi batch chỉ có một grade (A/B/C), không mix
**Áp dụng**: Receipt validation, batch creation

### LESSON-003: Phân quyền phải check BOTH role AND warehouse

**Biến cố**: [TBD] Chỉ check role → staff access kho không được assign
**Giải pháp**: Authorization = role permission + warehouse assignment
**Áp dụng**: All warehouse operations

### LESSON-004: Manual entry workflow

**Biến cố**: [TBD] Thiết bị quét không hiện tại, nên nhập liệu thủ công là chính
**Giải pháp**: Thiết kế giao diện nhập nhanh, kiểm tra dữ liệu tại chỗ và cho phép tìm kiếm sản phẩm bằng mã/SKU
**Áp dụng**: Receipt, Issue, Transfer screens

---


---

## DEVELOPMENT WORKFLOW

### Standard Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│   /spec  →  /plan  →  /build  →  /test  →  /review  →  /deploy     │
│   Define    Plan     Build     Verify    Review     Deploy         │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Workflow Commands

| Phase      | Command             | Purpose                                       |
| ---------- | ------------------- | --------------------------------------------- |
| **Define** | `speckit-specify`   | Create/update feature specification           |
| **Plan**   | `speckit-plan`      | Decompose into tasks with acceptance criteria |
| **Build**  | `speckit-implement` | Execute tasks incrementally                   |
| **Verify** | `tdd` skill         | TDD cycle (RED-GREEN-REFACTOR)                |
| **Review** | `code-review` skill | Five-axis code review                         |
| **Deploy** | `deploy` skill      | Build, test, deploy                           |

### Supporting Commands

| Command                | Use When                                    |
| ---------------------- | ------------------------------------------- |
| `/debug`               | Systematic error diagnosis                  |
| `/simplify`            | Reduce complexity without changing behavior |
| `speckit-analyze`      | Check spec consistency                      |
| `speckit-git-validate` | Validate branch naming                      |

---

## RULES & GUIDELINES

### ✅ ALWAYS DO

| Rule                   | Description                                    |
| ---------------------- | ---------------------------------------------- |
| **Input validation**   | Use Jakarta Validation annotations             |
| **Audit logging**      | Log all warehouse operations (who, when, what) |
| **Inventory check**    | Always check stock BEFORE issue                |
| **Structured logging** | Use SLF4J (no console.log/system.out)          |
| **Unit tests**         | Min 80% coverage for services                  |
| **API docs**           | Document in OpenAPI/Swagger                    |
| **Error handling**     | Proper HTTP status codes                       |
| **Comments**           | Explain WHY not WHAT                           |

### ❌ NEVER DO

| Rule                      | Description                              |
| ------------------------- | ---------------------------------------- |
| **No secrets**            | Never store passwords/keys in plain text |
| **No raw SQL**            | Always use JPA/Hibernate                 |
| **No negative inventory** | Prevent under all circumstances          |
| **No skip QC**            | Always check before warehouse receipt    |
| **No skip audit**         | Log all warehouse operations             |
| **No TODO left**          | Remove before merge                      |
| **No deprecated libs**    | Without team approval                    |

### Code Quality

| Metric              | Limit          |
| ------------------- | -------------- |
| Max function length | 40 lines       |
| Max file length     | 300 lines      |
| Min test coverage   | 80% (services) |

---

## NAMING CONVENTIONS

### Java (Backend)

| Type      | Convention  | Example                 |
| --------- | ----------- | ----------------------- |
| Classes   | PascalCase  | `WarehouseService.java` |
| Packages  | lowercase   | `com.wms.service`       |
| Tables    | snake_case  | `warehouse_staff`       |
| Constants | UPPER_SNAKE | `MAX_BATCH_QUANTITY`    |
| Methods   | camelCase   | `findByWarehouseId()`   |

### React (Frontend)

| Type       | Convention | Example                  |
| ---------- | ---------- | ------------------------ |
| Components | PascalCase | `WarehouseDashboard.jsx` |
| Hooks      | camelCase  | `useInventory.js`        |
| Utils      | camelCase  | `formatCurrency.js`      |

### API Routes

| Type         | Convention | Example                        |
| ------------ | ---------- | ------------------------------ |
| Endpoints    | kebab-case | `/api/v1/warehouse-stock`      |
| HTTP methods | lowercase  | `GET /api/v1/batch-management` |

---

## GIT CONVENTIONS

### Branch Naming

```
feat/[feature-name]      # New features (e.g., feat/inventory-FEFO)
fix/[bug-name]           # Bug fixes (e.g., fix/negative-stock)
spec/[feature-name]      # Specification work
chore/                   # Maintenance tasks
```

### Commit Format

```
[type]: [scope] - [description]

Types: feat, fix, docs, style, refactor, test, chore
Scopes: inventory, receipt, issue, transfer, batch, etc.

Examples:
feat(inventory): add FEFO batch selection logic
fix(batch): correct expiry date calculation
docs(api): update warehouse-stock endpoint docs
```

### Pull Request Rules

- Minimum 1 approval before merge
- Max 400 lines changed
- All CI checks must pass
- No TODO comments left

---

## GITNEXUS INTEGRATION

### Current Setup

- ✅ Repository: `Manager-warehouse-sdd`
- ✅ Indexed: 115 symbols, 111 relationships
- ✅ Repository-scoped query enabled

### Available Commands

```bash
# Query the current repo
gitnexus query "warehouse inventory" --limit 10

# Impact analysis (what breaks if I change X)
gitnexus impact --target WarehouseService --repo Manager-warehouse-sdd

# Sync index after code changes
gitnexus sync --repo Manager-warehouse-sdd

# Check status
gitnexus status --repo Manager-warehouse-sdd
```

### MCP Tools (when GitNexus MCP server is running)

| Tool                      | Purpose                                |
| ------------------------- | -------------------------------------- |
| `gitnexus_query`          | Find execution flows for a concept     |
| `gitnexus_impact`         | Blast radius analysis                  |
| `gitnexus_context`        | Full symbol context (callers, callees) |
| `gitnexus_detect_changes` | Map git changes to affected flows      |

### Workflow Integration

```python
# BEFORE editing a symbol:
1. Run gitnexus_impact({target: "symbolName", direction: "upstream"})
2. Report blast radius to user
3. If HIGH/CRITICAL risk → get confirmation before proceeding

# BEFORE committing:
1. Run gitnexus_detect_changes({scope: "staged"})
2. Verify only expected symbols affected
3. If unexpected → investigate before committing
```

---

## SEMBLE INTEGRATION

### What is Semble?

Semble is a semantic code search tool that finds code by **meaning**, not just text matching. It complements GitNexus by providing cross-repo search and similarity detection.

### Semble vs GitNexus - When to Use What?

| Task                                   | Tool         | Why                             |
| -------------------------------------- | ------------ | ------------------------------- |
| "Find all FEFO implementations"        | **Semble**   | Semantic search across codebase |
| "What breaks if I change X?"           | **GitNexus** | Call graph + impact analysis    |
| "Show me code similar to BatchService" | **Semble**   | Similarity detection            |
| "Who calls this method?"               | **GitNexus** | Relationship graph              |
| "Trace receipt flow end-to-end"        | **GitNexus** | Execution flow analysis         |
| "Find validation patterns"             | **Semble**   | Pattern search across files     |

### MCP Tools Available

| Tool                    | Purpose              | Example                                          |
| ----------------------- | -------------------- | ------------------------------------------------ |
| `semble.search()`       | Semantic code search | `semble.search("inventory quantity validation")` |
| `semble.find_related()` | Find similar code    | `semble.find_related("BatchService.selectFEFO")` |

### Optimal Workflow: Semble + GitNexus

#### Scenario 1: Adding New Feature

```
1. Semble search: Find existing similar implementations
   → semble.search("warehouse receipt validation")

2. GitNexus context: Understand how existing code is used
   → gitnexus_context("ReceiptService.validate")

3. GitNexus impact: Check blast radius before modifying
   → gitnexus_impact("ReceiptService")

4. Implement changes

5. GitNexus detect: Verify scope before commit
   → gitnexus_detect_changes()
```

#### Scenario 2: Bug Fixing

```
1. Semble search: Find all places with similar logic
   → semble.search("batch expiry date calculation")

2. GitNexus query: Trace execution flow
   → gitnexus_query("batch expiry validation")

3. GitNexus impact: Check what depends on the buggy code
   → gitnexus_impact("BatchService.calculateExpiry")

4. Fix bug

5. Semble find_related: Check if similar bugs exist elsewhere
   → semble.find_related("BatchService.calculateExpiry")
```

#### Scenario 3: Refactoring

```
1. Semble search: Find all code that needs refactoring
   → semble.search("manual inventory adjustment")

2. GitNexus impact: Assess blast radius
   → gitnexus_impact("InventoryService.adjust")

3. Refactor safely

4. GitNexus detect: Verify only expected changes
   → gitnexus_detect_changes()
```

### Semble CLI Commands

Semble CLI is installed at `/Users/haison/.local/bin/semble` and provides direct command-line access.

#### Available Commands

```bash
# Search codebase with natural language
semble search "authentication flow" .
semble search "FEFO batch selection logic" .

# Search for specific symbol or identifier
semble search "InventoryService" .

# Search remote repository (auto-clones)
semble search "warehouse management" https://github.com/example/repo

# Limit number of results
semble search "validation logic" . --top-k 10

# Include non-code files (markdown, yaml, json, etc.)
semble search "configuration" . --include-text-files

# Find code similar to a specific location
semble find-related src/service/InventoryService.java 42 .

# Show token savings statistics
semble savings

# Initialize semble sub-agent file
semble init
```

#### Practical Examples for WMS Project

```bash
# Find all FEFO implementations
semble search "FEFO batch selection" . --top-k 10

# Find inventory validation patterns
semble search "inventory quantity validation" .

# Find similar code to BatchService
semble find-related src/service/BatchService.java 1 .

# Search warehouse receipt flow
semble search "warehouse receipt process" .

# Find QC validation logic
semble search "quality control validation" .

# Include config files in search
semble search "database configuration" . --include-text-files
```

#### When to Use CLI vs MCP

| Scenario                    | Use CLI                      | Use MCP (via AI)          |
| --------------------------- | ---------------------------- | ------------------------- |
| Quick manual search         | ✅ `semble search "query" .` | ❌                        |
| Exploring codebase manually | ✅ Direct terminal           | ❌                        |
| AI-driven workflow          | ❌                           | ✅ AI calls MCP tools     |
| Integrated with GitNexus    | ❌                           | ✅ AI orchestrates both   |
| Part of automated task      | ❌                           | ✅ AI decides when to use |

**Recommendation**: Let AI use Semble MCP tools automatically. Use CLI only for manual exploration.

### Best Practices

- **Start with Semble** when exploring unfamiliar domain concepts
- **Use GitNexus** before any code modification to check impact
- **Combine both** for comprehensive understanding:
  - Semble finds WHAT exists
  - GitNexus shows HOW it's connected

### Combined CLI Reference

#### Exploration Phase

```bash
# Find code by concept (Semble)
semble search "FEFO batch selection" --limit 10

# Understand execution flow (GitNexus)
gitnexus query "warehouse receipt process"

# Get full context of a symbol (GitNexus)
gitnexus context --name "InventoryService.reserve"
```

#### Before Editing

```bash
# Check impact (GitNexus)
gitnexus impact --target "BatchService.selectFEFO" --direction upstream

# Find similar code that might need same change (Semble)
semble find-related "BatchService.selectFEFO"
```

#### After Editing

```bash
# Verify changes (GitNexus)
gitnexus detect-changes --scope staged

# Check if similar patterns exist elsewhere (Semble)
semble search "batch selection logic"
```

#### Refactoring

```bash
# Find all instances of a pattern (Semble)
semble search "inventory validation pattern"

# Safe rename across call graph (GitNexus)
gitnexus rename --from "oldMethodName" --to "newMethodName"
```

---

## SPECKIT INTEGRATION

### What is Speckit?

Speckit is an AI-driven Spec-Driven Development (SDD) tool that automates the workflow from specification to implementation. It follows a structured process: Define → Plan → Build → Test → Review.

### Speckit MCP Server Setup

Speckit works as an MCP (Model Context Protocol) server that needs to be configured in your IDE.

#### Installation via NPX (Recommended)

1. **Configure Cursor IDE** - Edit MCP config file:
   - macOS: `~/.cursor/mcp.json`
   - Linux: `~/.config/cursor/mcp.json`
   - Windows: `%APPDATA%\Cursor\mcp.json`

2. **Add Speckit server**:

```json
{
  "mcpServers": {
    "speckit": {
      "command": "npx",
      "args": ["-y", "speckit-mcp-x@latest"]
    }
  }
}
```

3. **Restart Cursor** - Speckit will auto-download on first use

#### Requirements

- Python 3.10+ (installed: Python 3.11.15 ✅)
- Node.js with npx

### Available Speckit Tools (via MCP)

| Tool                   | Purpose                             | Example                                  |
| ---------------------- | ----------------------------------- | ---------------------------------------- |
| `speckit_specify`      | Create/update feature specification | Define new feature requirements          |
| `speckit_plan`         | Decompose spec into tasks           | Break down feature into actionable steps |
| `speckit_implement`    | Execute tasks incrementally         | Build feature step-by-step               |
| `speckit_analyze`      | Check spec consistency              | Validate specification completeness      |
| `speckit_git_validate` | Validate branch naming              | Ensure git conventions                   |

### Speckit Workflow Integration

#### Standard SDD Flow

```
1. /spec (speckit_specify)
   → Define feature specification in /specs directory
   → Document requirements, acceptance criteria, constraints

2. /plan (speckit_plan)
   → Decompose spec into tasks with clear steps
   → Assign priorities and dependencies

3. /build (speckit_implement)
   → Execute tasks incrementally
   → Follow TDD: RED → GREEN → REFACTOR

4. /test
   → Run unit tests (min 80% coverage)
   → Run integration tests

5. /review
   → Five-axis code review
   → Check against spec requirements

6. /deploy
   → Build, test, deploy
```

### Combining Speckit + GitNexus + Semble

#### Scenario: Adding New Feature with Full Workflow

```
1. SPEC PHASE (Speckit)
   → speckit_specify: Create spec for "FEFO batch selection"
   → Document business rules, edge cases

2. EXPLORATION PHASE (Semble + GitNexus)
   → semble.search("batch selection logic"): Find similar implementations
   → gitnexus_query("inventory management"): Understand existing flows

3. PLANNING PHASE (Speckit)
   → speckit_plan: Break down into tasks
   → Task 1: Add FEFO selector utility
   → Task 2: Update IssueService to use FEFO
   → Task 3: Add unit tests

4. IMPACT ANALYSIS (GitNexus)
   → gitnexus_impact("IssueService"): Check blast radius
   → Verify who calls IssueService methods

5. IMPLEMENTATION (Speckit)
   → speckit_implement: Execute tasks one by one
   → TDD cycle for each task

6. VERIFICATION (GitNexus)
   → gitnexus_detect_changes(): Verify scope
   → Ensure only expected symbols affected

7. VALIDATION (Speckit)
   → speckit_analyze: Check spec compliance
   → All acceptance criteria met?
```

### Best Practices

**When to use Speckit:**

- Starting new features (always begin with /spec)
- Complex features requiring structured planning
- Team collaboration (specs as documentation)
- Ensuring requirements traceability

**When to combine with GitNexus:**

- Before modifying existing code (impact analysis)
- Understanding how new feature fits into existing architecture
- Refactoring (safe rename, detect changes)

**When to combine with Semble:**

- Finding existing patterns to follow
- Discovering similar code that might need updates
- Learning from existing implementations

### Project Structure for Speckit

```
specs/
├── 001-featurename-us-wh/
│   ├── spec.md                    # Main specification
│   ├── plan.md                    # Task breakdown
│   └── implementation-log.md      # Progress tracking
├── 002-fefo-batch-selection/      # (example next spec)
│   ├── spec.md
│   └── plan.md
└── README.md                      # Specs index
```

### Speckit Commands Reference

```bash
# Note: These are MCP tools, not CLI commands
# They are invoked by AI through the MCP protocol

# Create specification
speckit_specify({
  feature: "FEFO batch selection",
  requirements: "...",
  acceptance_criteria: "..."
})

# Generate plan
speckit_plan({
  spec_path: "/specs/002-fefo-batch-selection/spec.md"
})

# Implement tasks
speckit_implement({
  plan_path: "/specs/002-fefo-batch-selection/plan.md",
  task_id: "task-1"
})

# Validate spec
speckit_analyze({
  spec_path: "/specs/002-fefo-batch-selection/spec.md"
})
```

### Integration with AGENTS.md

The project follows Speckit conventions as defined in AGENTS.md:

- Specs stored in `/specs` directory
- Standard workflow: spec → plan → build → test → review
- Git branch naming: `spec/[feature-name]`
- Commit format includes spec reference

## QUICK REFERENCE

### Core Entities

```
Warehouse (3 locations: Hải Phòng, Hà Nội, Hồ Chí Minh)
├── Zones (receiving, storage, picking, shipping, quarantine)
└── Locations (zone-bin, bin-capacity)

Product (1000+ items)
├── SKU, name, unit, dimension, weight
└── PriceHistory (cost_price, selling_price, effective_date, end_date)

Batch (Lô hàng - tied to ONE grade)
├── batchNumber, receivedDate, expDate (optional; only for exceptional expiry-tracked products)
├── grade (A/B/C)
└── quantity

Inventory (tồn kho)
├── warehouse + product + batch + location
└── quantity (NEVER negative)

Receipt (Lệnh nhập kho / Phiếu nhập kho)
├── receiptNumber, sourceOrderCode, type (purchase/return)
├── warehouse, supplier, status (pending_receipt/draft/qc_completed/approved/rejected)
└── items (product + quantity + QC grade)

Issue (Đơn xuất hàng / Phiếu xuất kho)
├── issueNumber, customer/dealer, type (sale/delivery/adjustment)
├── warehouse, status (new/picking/ready_to_ship/in_transit/delivered/completed/closed)
└── items (product + quantity)

Transfer (Phiếu điều chuyển kho)
├── source → destination warehouses
├── status (new/approved/in_transit/completed/completed_with_discrepancy/cancelled)
└── In-Transit virtual location

Invoice (Hóa đơn bán hàng)
├── invoiceNumber, dealer, total_amount, issue_date, due_date
└── status (unpaid/paid)

PaymentReceipt (Phiếu thu tiền)
├── paymentNumber, dealer, amount, payment_date, payment_method
└── applied_invoices

CreditNote (Phiếu ghi giảm công nợ)
└── dealer, amount, reason (returns)

DebitNote (Phiếu đòi bồi hoàn)
└── supplier, amount, reason (QC rejected goods)
```

### Key Business Rules

| Rule                       | Implementation                                  |
| -------------------------- | ----------------------------------------------- |
| No negative inventory      | `@Column(check = "quantity >= 0")` + validation |
| Single grade per batch     | `grade` is immutable after creation             |
| Household goods default    | Products such as pots, pans, and plastic goods do not track expiry by default |
| FIFO default               | `FIFOSelector` picks batch by receivedDate ASC for the current household-goods domain |
| FEFO exception             | `FEFOSelector` is used only for exceptional products configured with expiry tracking |
| Quarantine excluded        | WHERE clause filters `zone != 'QUARANTINE'`     |
| In-Transit tracking        | Virtual warehouse `IN_TRANSIT` for transfers    |
| Credit Check Control       | Auto-block if balance + new > limit OR >30 days overdue; balance equal to limit is allowed. Buffer 20% to unlock |
| Monthly Closing            | Lock previous monthly periods (CLOSED), only Adjustment Vouchers allowed in open period |
| Phúc Anh Internal Fleet    | All deliveries use internal fleet & drivers. No 3PL or delivery cost approvals |

### Actor Reference (10 Actors — xem chi tiết tại `Kiến trúc phân tầng các Actors.md`)

| Tầng | Actor | Loại | Trách nhiệm chính |
|---|---|---|---|
| Quản trị | CEO | Checker | Dashboard chiến lược |
| Quản trị | System Admin | Admin | Quản lý tài khoản, RBAC, cấu hình tham số hệ thống |
<<<<<<< HEAD
| Quản lý | Trưởng kho kiêm Trưởng QC | Checker | Duyệt nhập/xuất/điều chuyển, xử lý chênh lệch thực tế, duyệt biên bản hàng lỗi |
=======
| Quản lý | Trưởng kho | Checker | Duyệt nhập/xuất/điều chuyển, xử lý chênh lệch 5M–100M, duyệt biên bản xử lý hàng lỗi |
>>>>>>> 78bb76f (update spec master data and database, entity)
| Quản lý | Kế toán trưởng | Checker | Duyệt bảng giá, thiết lập Credit Limit, chốt sổ tháng, P&L/Aging Report |
| Nghiệp vụ | Planner | Maker | Lập lệnh nhập / đơn xuất từ Công ty mẹ, kiểm tra Credit Check + tồn kho |
| Nghiệp vụ | Dispatcher | Maker | Lập chuyến xe nội bộ Phúc Anh, gán tài xế, tối ưu lộ trình giao hàng |
| Nghiệp vụ | Thủ kho kiêm QC | Maker | Tiếp nhận hàng, kiểm QC inbound/outbound, soạn hàng, kiểm kê, cất Bin, xác nhận điều chuyển |
| Nghiệp vụ | Nhân viên kho (Bốc xếp) | Maker | Bốc xếp hàng hóa, hỗ trợ di chuyển hàng hóa, di chuyển hàng lỗi vào Quarantine theo chỉ dẫn |
| Nghiệp vụ | Kế toán viên | Maker | Lập hóa đơn, ghi nhận thanh toán, cấn trừ công nợ, quản lý bảng giá |
| Nghiệp vụ | Tài xế | Maker | Nhận chuyến (smartphone), giao hàng, xác nhận POD, báo giao thất bại |

> **Lưu ý phân biệt Dispatcher vs Planner**: Planner = nhận đơn từ Công ty mẹ & lập DO; Dispatcher = điều phối xe & tài xế giao hàng. Hai vai trò khác nhau hoàn toàn.

### Current Sprint

- **Sprint 1**: Core Warehouse Operations & Internal Accounting
- **Focus**: Inventory, Receipt, Issue, Transfer, Credit Control, Internal Price Lists
- **Active specs**: `specs/001-featurename-us-wh/spec.md`

---

## MCP ARCHITECTURE OVERVIEW

### Data Flow

```
User Input → AI Model → MCP Client → MCP Server → External Service
                    ↑                     ↓
                    └───── JSON-RPC ─────┘
```

### Security Principles

| Principle           | Application                    |
| ------------------- | ------------------------------ |
| **Least Privilege** | Only request necessary scopes  |
| **Token Rotation**  | Rotate MCP tokens regularly    |
| **Audit Logging**   | Log all MCP tool usage         |
| **Validation**      | Validate all inputs before use |

---

<!-- Claude Code auto-appends entries below when working -->
<!-- Last updated: 2026-05-25 -->

---

## SWIMLANE DIAGRAMS

Các sơ đồ swimlane dưới đây mô tả chính xác quy trình nghiệp vụ thực tế của WMS Phúc Anh theo phân vai 10 Actors. Các quy trình sử dụng đội xe nội bộ Phúc Anh, cơ chế Credit Check tự động và phân quyền phê duyệt chặt chẽ.

### 1. Quy trình Nhập hàng (Receipt Process)

Quy trình nhập hàng từ khi Công ty mẹ gửi thông tin qua Zalo/Email cho tới khi hàng được cất vào Bin hoặc khu Quarantine và được Trưởng kho phê duyệt để hệ thống cập nhật số tồn khả dụng.

```
┌─────────────┬─────────────┬─────────────────┬─────────────┬────────────────┐
│   PLANNER   │   THỦ KHO   │  NHÂN VIÊN KHO  │ TRƯỞNG KHO  │     SYSTEM     │
├─────────────┼─────────────┼─────────────────┼─────────────┼────────────────┤
│             │             │                 │             │                │
│ Lập lệnh    │             │                 │             │                │
│ [Pending]───►             │                 │             │                │
│             │ Nhận lệnh,  │                 │             │                │
│             │ đếm hàng,   │                 │             │                │
│             │ nhập nháp───►                 │             │                │
│             │             │ Kiểm ngoại quan,│             │                │
│             │             │ nhập KQ Đạt/Lỗi │             │                │
│             │             │        │        │             │                │
│             │             │  [HÀNG LỖI]     │             │                │
│             │             │        ├────────► Biên bản lỗi │                │
│             │             │        │        │ Quyết định  │                │
│             │             │        │        │ xử lý ──────►                │
│             │             │  [HÀNG ĐẠT]     │             │                │
│             │             │        ├────────► Phê duyệt   │                │
│             │             │        │        │ Phiếu nhập──►                │
│             │             │        │        │             │ Cộng tồn kho   │
│             │             │        │        │             │ khả dụng &     │
│             │             │        │        │             │ Quarantine zone│
│             │             │                 │             │                │
└─────────────┴─────────────┴─────────────────┴─────────────┴────────────────┘
```

**Luồng trạng thái đơn nhập:**
`PENDING_RECEIPT` (Planner tạo) → `DRAFT` (Thủ kho kiểm đếm) → `QC_COMPLETED` (Thủ kho hoàn tất QC) → `APPROVED` (Trưởng kho duyệt, Hệ thống cộng tồn kho) / `REJECTED` (Trưởng kho từ chối, xuất hủy hoặc trả NCC)

---

### 2. Quy trình Xuất hàng (Issue Process)

Quy trình xử lý đơn xuất hàng bán cho Đại lý, tích hợp kiểm tra công nợ (Credit Check) tự động trước khi cho phép soạn hàng.

```
┌─────────────┬────────────────────────────────┬─────────────┬───────────────┐
│   PLANNER   │             SYSTEM             │   THỦ KHO   │ NHÂN VIÊN KHO │
├─────────────┼────────────────────────────────┼─────────────┼───────────────┤
│             │                                │             │               │
│ Lập Đơn xuất│                                │             │               │
│ (DO) ──────►│ Tự động check công nợ Đại lý   │             │               │
│             │ ├── VI PHẠM: Chặn cứng, báo lỗi│             │               │
│             │ └── HỢP LỆ: Tạo DO [New],      │             │               │
│             │             giữ hàng (Reserve) │             │               │
│             │             └─────────────────►│             │               │
│             │                                │ Soạn hàng   │               │
│             │                                │ từ Bin, set │               │
│             │                                │ [Picking]───►               │
│             │                                │ Kiểm QC sản │               │
│             │                                │ phẩm đã soạn│               │
│             │                                │ xác nhận đạt│               │
│             │                                │ Xác nhận    │               │
│             │                                │ soạn xong,  │               │
│             │                                │ set [Ready] │               │
│             │                                │             │               │
└─────────────┴────────────────────────────────┴─────────────┴───────────────┘
```

**Luồng trạng thái đơn xuất:**
`NEW` (Planner lập đơn & System check công nợ đạt) → `PICKING` (Thủ kho bắt đầu soạn hàng từ Bin) → `READY_TO_SHIP` (Thủ kho xác nhận QC & đóng gói đạt, hoàn thành soạn hàng)

---

### 3. Quy trình Điều chuyển Kho Nội bộ (Internal Transfer)

Quy trình điều phối hàng hóa giữa 3 kho vật lý Hải Phòng, Hà Nội, TP.HCM thông qua kho ảo trung chuyển `IN-TRANSIT` bằng xe nội bộ của Phúc Anh.

```
┌─────────────┬───────────────┬────────────────┬────────────────┬────────────┐
│   PLANNER   │ TRƯỞNG KHO N  │ THỦ KHO NGUỒN  │     SYSTEM     │TRƯỞNG KHO Đ│
├─────────────┼───────────────┼────────────────┼────────────────┼────────────┤
│             │               │                │                │            │
│ Tạo Phiếu   │               │                │                │            │
│ điều chuyển │               │                │                │            │
│ [Mới] ─────►│ Duyệt điều    │                │                │            │
│             │ chuyển ──────►│                │                │            │
│             │               │ Ghi số gửi,    │                │            │
│             │               │ bốc xếp xe ───►│ Chờ tài xế     │            │
│             │               │                │ xác nhận       │            │
│             │               │                │                │            │
│             │               │ Tài xế xác nhận│ Trừ tồn kho N, │            │
│             │               │ xe rời kho ───►│ cộng In-Transit│            │
│             │               │                │ └──────────────► Đếm hàng   │
│             │               │                │                │ số nhận, QC│
│             │               │                │                │ chất lượng,│
│             │               │                │                │ đối chiếu  │
│             │               │                │                │     │      │
│             │               │                │                │   [KHỚP]   │
│             │               │                │                │     ├─────►│
│             │               │                │ Trừ In-Transit,│            │
│             │               │                │ cộng kho Đích  │            │
│             │               │                │ ◄──────────────┤            │
│             │               │                │                │   [LỆCH]   │
│             │               │                │                │     ├─────►│
│             │               │                │ Ghi lý do lệch │            │
│             │               │                │ tạo Adjustment │            │
│             │               │                │ QC lỗi →       │            │
│             │               │                │ Quarantine     │            │
│             │               │                │ ◄──────────────┤            │
│             │               │                │                │            │
└─────────────┴───────────────┴────────────────┴────────────────┴────────────┘
```

**Luồng trạng thái phiếu điều chuyển:**
`NEW` (Planner tạo thủ công từ mã lệnh Công ty mẹ/bộ phận điều phối) → `APPROVED` (Trưởng kho nguồn duyệt và khóa hàng ngay) → `IN_TRANSIT` (Dispatcher đã lập chuyến xe riêng, Thủ kho nguồn ghi đúng số gửi đã duyệt, Tài xế xác nhận nhận hàng và xe rời kho; hệ thống dịch chuyển tồn kho vào kho trung chuyển `IN_TRANSIT` và trừ tồn kho nguồn) → `COMPLETED` (Công nhân kho đích nhập số nhận ban đầu, Thủ kho đích kiểm lại số lượng + QC + chọn vị trí nhập, Trưởng kho đích xác nhận cuối cùng, khớp số lượng và QC đạt) / `COMPLETED_WITH_DISCREPANCY` (Nhận thiếu, tạo phiếu điều chỉnh bù trừ và log audit). Nếu hàng đã được ghi lên xe nhưng chưa rời kho, muốn hủy phải unship/unload trước để quay về trạng thái hủy được. Nếu `received_qty > sent_qty` hệ thống chặn xác nhận; nếu QC lỗi thì phần lỗi vào Quarantine và không tính available. Không hỗ trợ hủy phiếu sau khi đã `IN_TRANSIT`.

**Quy tắc chuyến xe điều chuyển:**
- Mỗi Phiếu điều chuyển gắn đúng một chuyến xe nội bộ riêng (`trips.trip_type = TRANSFER`).
- Không gom nhiều Phiếu điều chuyển vào một chuyến xe trong Sprint 1.
- Tài xế phải xác nhận đã nhận hàng và xe rời kho trước khi hệ thống chuyển tồn sang `IN_TRANSIT`.

---

### 4. Quy trình Giao hàng (Delivery Process)

Quy trình điều phối chuyến xe, vận chuyển bằng xe nội bộ của công ty và cập nhật trạng thái đơn hàng kèm chữ ký điện tử / ảnh chụp POD của Đại lý trên thiết bị di động của Tài xế.

```
┌───────────────┬───────────────────────────────────────────────┬────────────┐
│  DISPATCHER   │                    TÀI XẾ                     │   SYSTEM   │
├───────────────┼───────────────────────────────────────────────┼────────────┤
│               │                                               │            │
│ Gom đơn [Ready│                                               │            │
│ to Ship], gán │                                               │            │
│ xe nội bộ ───►│ Nhận chuyến qua Smartphone,                   │            │
│               │ xác nhận bốc xếp hàng, rời kho ──────────────►│ Trừ tồn kho│
│               │                                               │ set trạng  │
│               │                                               │ thái đơn   │
│               │                                               │ [IN_TRANSIT]│
│               │                                               │            │
│               │ Đến điểm giao, bàn giao hàng cho Đại lý       │            │
│               │        │                                      │            │
│               │  [GIAO THÀNH CÔNG]                            │            │
│               │        ├─────────────────────────────────────►│ Lưu POD &  │
│               │        │ Ký tên trên màn hình, chụp ảnh       │ set đơn    │
│               │        │                                      │ [Delivered]│
│               │  [GIAO THẤT BẠI]                              │            │
│               │        ├─────────────────────────────────────►│ Nhập hoàn  │
│               │        │ Ghi lý do (vắng, từ chối, sai...)     │ Quarantine,│
│               │        │                                      │ set trạng  │
│               │        │                                      │ thái đơn   │
│               │        │                                      │ [Returned] │
│               │                                               │            │
└───────────────┴───────────────────────────────────────────────┴────────────┘
```

**Luồng trạng thái đơn giao:**
`READY_TO_SHIP` → `IN_TRANSIT` (Tài xế nhận hàng lên xe và đang đi giao) → `DELIVERED` (Đại lý ký nhận POD thành công) / `RETURNED` (Giao thất bại, chuyển hàng về Quarantine zone của kho gốc)

---

### 5. Chu kỳ Tài chính & Kiểm soát Công nợ (Finance & Credit Cycle)

Chu kỳ lập Hóa đơn bán hàng, theo dõi hạn mức công nợ (Credit Limit), chặn nợ quá hạn và thực hiện chốt sổ kế toán tháng (Monthly Closing).

```
┌─────────────────────────────────┬────────────────────────────────┬─────────┐
│          KẾ TOÁN VIÊN           │             SYSTEM             │ KTT/CEO │
├─────────────────────────────────┼────────────────────────────────┼─────────┤
│                                 │                                │         │
│ Nhận đơn Delivered, lập Invoice │                                │         │
│ (Net 30/60) ───────────────────►│ Cộng dồn current_balance,      │         │
│                                 │ set trạng thái [Completed]     │         │
│                                 │                                │         │
│                                 │ [Credit Check khi tạo đơn mới] │         │
│                                 │ IF balance + DO > limit OR     │         │
│                                 │ overdue > 30 ngày -> HOLD ─────► Kính báo│
│                                 │                                │ KTT/CEO │
│ Đại lý thanh toán, lập Phiếu thu│                                │         │
│ cấn trừ hóa đơn ───────────────►│ Trừ current_balance.           │         │
│                                 │ IF balance < limit * 0.8       │         │
│                                 │ -> Mở khóa tín dụng (ACTIVE)   │         │
│                                 │                                │         │
│                                 │                                │ Chốt sổ │
│                                 │                                │ tháng   │
│                                 │◄───────────────────────────────┤ (Close) │
│                                 │ Khóa cứng mọi chứng từ kỳ T,   │         │
│                                 │ chỉ cho phép Adjustment ở kỳ T1│         │
│                                 │                                │         │
└─────────────────────────────────┴────────────────────────────────┴─────────┘
```

**Các trạng thái kiểm soát công nợ Đại lý:**
- `ACTIVE`: Hạn mức công nợ hợp lệ, không có hóa đơn quá hạn quá 30 ngày. Cho phép đặt đơn hàng mới bình thường.
- `CREDIT_HOLD`: Khóa tín dụng khi vi phạm bất kỳ điều kiện nào (vượt hạn mức hoặc quá hạn nợ > 30 ngày). Hệ thống tự động chặn lập đơn mới.
- **Mở khóa**: Khi Đại lý thanh toán đưa `current_balance` về mức an toàn dưới `credit_limit * 0.8` (đệm an toàn 20%) và xử lý toàn bộ hóa đơn nợ quá hạn.

---

### 6. Quy trình Kiểm kê kho (StockTake Process)

Quy trình đối chiếu số liệu tồn kho thực tế, tính toán chênh lệch và phân cấp thẩm quyền phê duyệt điều chỉnh giá trị chênh lệch.

```
┌─────────────┬─────────────────────────────────┬──────────────┐
│   THỦ KHO   │             SYSTEM              │  TRƯỞNG KHO  │
├─────────────┼─────────────────────────────────┼──────────────┤
│             │                                 │              │
│ Tạo phiếu   │                                 │              │
│ kiểm kê,    │                                 │              │
│ đếm hàng,   │                                 │              │
│ nhập KQ ───►│ Tự động tính chênh lệch         │              │
│             │ (số lượng)                      │              │
│             │        │                        │              │
│             │        ├───────────────────────►│ Xem xét,     │
│             │        │                        │ duyệt        │
│             │        │◄───────────────────────┤ điều chỉnh   │
│             │                                 │              │
│             │ Cập nhật tồn kho theo thực tế,  │              │
│             │ ghi log và audit trail chi tiết │              │
│             │                                 │              │
└─────────────┴─────────────────────────────────┴──────────────┘
```

**Thẩm quyền duyệt chênh lệch kiểm kê & xuất hủy hàng lỗi:**
<<<<<<< HEAD
- Tất cả chênh lệch kiểm kê và phiếu xuất hủy hàng lỗi đều do Trưởng kho phê duyệt trực tiếp trên hệ thống.
=======
- **Từ 5 triệu đến 100 triệu VNĐ**: Trưởng kho xem xét và phê duyệt trên hệ thống.
- **Trên 100 triệu VNĐ hoặc lỗi xác định do nhân viên**: Phải trình trực tiếp CEO phê duyệt trên hệ thống.
>>>>>>> 78bb76f (update spec master data and database, entity)

---

## ANTI-PATTERNS (Tránh xa)

### Code Anti-Patterns

| Anti-Pattern               | Description                                        | How to Avoid                                |
| -------------------------- | -------------------------------------------------- | ------------------------------------------- |
| **God Class/Service**      | Một class/service quá lớn, làm mọi thứ             | Max 300 lines/file, 40 lines/function       |
| **Anemic Domain Model**    | Entities chỉ có getters/setters, không có behavior | Move logic vào domain methods               |
| **Circular Dependencies**  | A → B → C → A                                      | Use interfaces, dependency injection        |
| **Premature Optimization** | Tối ưu sớm, phức tạp hóa code không cần thiết      | YAGNI - You Ain't Gonna Need It             |
| **Magic Numbers**          | Hard-coded numbers không có constant               | Use named constants (e.g., `MAX_RETRY = 3`) |
| **Dead Code**              | Code không bao giờ được gọi                        | Remove unused code, keep clean              |
| **Shotgun Surgery**        | Một thay đổi cần sửa nhiều file không liên quan    | Keep related code together                  |

### Database Anti-Patterns

| Anti-Pattern                     | Description                                | How to Avoid                              |
| -------------------------------- | ------------------------------------------ | ----------------------------------------- |
| **N+1 Query Problem**            | 1 query, rồi N queries cho mỗi item        | Use JOIN FETCH, @EntityGraph              |
| **Missing Indexes**              | Chậm khi query lớn                         | Analyze queries, add indexes              |
| **Denormalization Abuse**        | Quá nhiều denormalized tables              | Balance read vs write performance         |
| **Soft Deletes Everywhere**      | Xóa mềm mọi thứ                            | Only soft delete audit-critical data      |
| **EAV (Entity-Attribute-Value)** | Dynamic attributes = performance nightmare | Use proper columns, JSON for true dynamic |

### Spring Boot Anti-Patterns

| Anti-Pattern             | Description                                     | How to Avoid                              |
| ------------------------ | ----------------------------------------------- | ----------------------------------------- |
| **Anemic Services**      | Service chỉ gọi repository, không có logic      | Add business logic to service layer       |
| **God Controllers**      | Controller xử lý quá nhiều, validate, transform | Use DTOs, validation annotations          |
| **Transaction Failures** | @Transactional không used đúng                  | Understand propagation, isolation levels  |
| **N+1 in JPA**           | Lazy loading gây N+1                            | Use @Fetch(FetchMode.JOIN) or JOIN FETCH  |
| **Oversized @Entity**    | Entity có 50+ columns                           | Split into smaller entities/Value Objects |

### React Anti-Patterns

| Anti-Pattern                | Description                              | How to Avoid                             |
| --------------------------- | ---------------------------------------- | ---------------------------------------- |
| **Prop Drilling**           | Pass props qua nhiều levels không cần    | Use Context or Zustand store             |
| **Memory Leaks**            | Unmounted component still listens        | Cleanup in useEffect return              |
| **Inline Functions in JSX** | `<button onClick={() => handleClick()}>` | Define outside render or use useCallback |
| **Overfetching**            | Fetch entire data when only need summary | Use GraphQL fields or API filters        |
| **God Components**          | One component 1000+ lines                | Split into smaller, focused components   |

### Integration Anti-Patterns

| Anti-Pattern               | Description                         | How to Avoid                             |
| -------------------------- | ----------------------------------- | ---------------------------------------- |
| **Chatty APIs**            | Nhiều API calls nhỏ thay vì batch   | Batch operations, composite APIs         |
| **Synchronous Everything** | Blocking calls for async operations | Use events, queues for long-running ops  |
| **Ignoring Failures**      | Catch exception, do nothing         | Always handle exceptions properly        |
| **Hard-coded Timeouts**    | Magic numbers for timeouts          | Use configuration with sensible defaults |

---

## TESTING ANTI-PATTERNS TO AVOID

| Anti-Pattern             | Description                           | Fix                                   |
| ------------------------ | ------------------------------------- | ------------------------------------- |
| **Test for coverage**    | Viết test chỉ để pass coverage metric | Write meaningful tests                |
| **Brittle selectors**    | Test break when refactor UI           | Use data-testid attributes            |
| **No assertion**         | Tests that only execute code          | Always assert expected outcomes       |
| **Shared mutable state** | Tests affect each other               | Each test = independent               |
| **Slow tests**           | Unit tests hitting real DB            | Mock dependencies, use testcontainers |

---
