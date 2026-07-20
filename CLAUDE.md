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
4. `.specify/memory/constitution.md`

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
│   - FIFO batch selection                │  - Audit logging
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

### LESSON-002: Batch does not classify household goods by grade

**Biến cố**: [TBD] Quy tắc truy vết từng cái, hạn dùng và phân cấp chất lượng làm nhập liệu quá nặng cho đơn hàng gia dụng số lượng lớn
**Giải pháp**: Batch theo sản phẩm + nguồn nhập/ngày nhận; hàng lỗi QC đi Quarantine để RTV/disposal, không phân cấp chất lượng để bán lại
**Áp dụng**: Receipt validation, batch creation, QC failed handling

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
feat/[feature-name]      # New features (e.g., feat/inventory-fifo)
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
feat(inventory): add FIFO batch selection logic
fix(batch): correct received date ordering
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
| "Find all FIFO implementations"        | **Semble**   | Semantic search across codebase |
| "What breaks if I change X?"           | **GitNexus** | Call graph + impact analysis    |
| "Show me code similar to BatchService" | **Semble**   | Similarity detection            |
| "Who calls this method?"               | **GitNexus** | Relationship graph              |
| "Trace receipt flow end-to-end"        | **GitNexus** | Execution flow analysis         |
| "Find validation patterns"             | **Semble**   | Pattern search across files     |

### MCP Tools Available

| Tool                    | Purpose              | Example                                          |
| ----------------------- | -------------------- | ------------------------------------------------ |
| `semble.search()`       | Semantic code search | `semble.search("inventory quantity validation")` |
| `semble.find_related()` | Find similar code    | `semble.find_related("BatchService.selectFIFO")` |

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
   → semble.search("batch received date ordering")

2. GitNexus query: Trace execution flow
   → gitnexus_query("FIFO batch selection validation")

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
semble search "FIFO batch selection logic" .

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
# Find all FIFO implementations
semble search "FIFO batch selection" . --top-k 10

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
semble search "FIFO batch selection" --limit 10

# Understand execution flow (GitNexus)
gitnexus query "warehouse receipt process"

# Get full context of a symbol (GitNexus)
gitnexus context --name "InventoryService.reserve"
```

#### Before Editing

```bash
# Check impact (GitNexus)
gitnexus impact --target "BatchService.selectFIFO" --direction upstream

# Find similar code that might need same change (Semble)
semble find-related "BatchService.selectFIFO"
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
   → speckit_specify: Create spec for "FIFO batch selection"
   → Document business rules, edge cases

2. EXPLORATION PHASE (Semble + GitNexus)
   → semble.search("batch selection logic"): Find similar implementations
   → gitnexus_query("inventory management"): Understand existing flows

3. PLANNING PHASE (Speckit)
   → speckit_plan: Break down into tasks
   → Task 1: Add FIFO selector utility
   → Task 2: Update IssueService to use FIFO
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
  feature: "FIFO batch selection",
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

Batch (Lô hàng)
├── batchNumber, receivedDate, sourceReceipt/sourceDocument
└── quantity

Inventory (tồn kho)
├── warehouse + product + batch + location
└── quantity (NEVER negative)

Receipt (Lệnh nhập kho / Phiếu nhập kho)
├── receiptNumber, sourceOrderCode, type (purchase/return)
├── warehouse, supplier, status (pending_receipt/draft/qc_completed/qc_failed/approved/return_to_supplier_pending/returned_to_supplier)
└── items (product + quantity + QC result)

Issue (Đơn xuất hàng / Phiếu xuất kho)
├── issueNumber, customer/dealer, type (sale/delivery/adjustment)
├── warehouse, status (new/waiting_picking/qc_pending_approval/qc_completed/warehouse_approved/in_transit/completed/closed)
└── items (product + quantity)

Transfer (Phiếu điều chuyển kho)
├── source → destination warehouses
├── status (new/approved/rejected/in_transit/completed/completed_with_discrepancy/cancelled)
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
| No negative inventory      | DB CHECK on `total_qty`, `reserved_qty`, and available quantity + service validation |
| Household goods traceability | Track by SKU, receipt document/date, batch, quantity, and location; no per-unit tracking, expiry dates, or quality tiers |
| Household goods default    | Products such as pots, pans, and plastic goods use receipt date traceability |
| FIFO default               | `FIFOSelector` picks batch by receivedDate ASC for the current household-goods domain |
| Quarantine excluded        | WHERE clause filters `zone != 'QUARANTINE'`     |
| In-Transit tracking        | Virtual warehouse `IN_TRANSIT` for transfers    |
| Credit Check Control       | Auto-block if balance + new > limit OR >30 days overdue; balance equal to limit is allowed. Buffer 20% to unlock |
| Monthly Closing            | Lock previous monthly periods (CLOSED), only Adjustment Vouchers allowed in open period |
| Phúc Anh Internal Fleet    | All deliveries use internal fleet & drivers. No 3PL or delivery cost approvals |

### Actor Reference (10 Actors — xem chi tiết tại `Kiến trúc phân tầng các Actors.md`)

| Tầng      | Actor                   | Loại    | Trách nhiệm chính                                                                           |
| --------- | ----------------------- | ------- | ------------------------------------------------------------------------------------------- |
| Quản trị  | CEO                     | Checker | Dashboard chiến lược                                                                        |
| Quản trị  | System Admin            | Admin   | Quản lý tài khoản, RBAC, cấu hình tham số hệ thống                                          |
| Quản lý   | Trưởng kho              | Checker | Duyệt nhập/xuất/điều chuyển, xử lý chênh lệch 5M–100M, duyệt biên bản xử lý hàng lỗi        |
| Quản lý   | Kế toán trưởng          | Checker | Duyệt bảng giá, thiết lập Credit Limit, chốt sổ tháng, P&L/Aging Report                     |
| Nghiệp vụ | Planner                 | Maker   | Lập lệnh nhập / đơn xuất từ Công ty mẹ, kiểm tra Credit Check + tồn kho                     |
| Nghiệp vụ | Dispatcher              | Maker   | Lập chuyến xe nội bộ Phúc Anh, gán tài xế, tối ưu lộ trình giao hàng                        |
| Nghiệp vụ | Thủ kho kiêm QC         | Maker   | Tiếp nhận hàng, kiểm QC inbound/outbound, soạn hàng, kiểm kê, cất Bin, xác nhận điều chuyển |
| Nghiệp vụ | Nhân viên kho (Bốc xếp) | Maker   | Bốc xếp hàng hóa, hỗ trợ di chuyển hàng hóa, di chuyển hàng lỗi vào Quarantine theo chỉ dẫn |
| Nghiệp vụ | Kế toán viên            | Maker   | Xử lý thanh toán và theo dõi công nợ trong luồng tài chính riêng, quản lý bảng giá |
| Nghiệp vụ | Tài xế                  | Maker   | Nhận chuyến (smartphone), upload `goodsImage`/`signDocumentImage`, nhập OTP Đại lý, báo giao thất bại, xác nhận xe về kho |

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

Quy trình nhập hàng từ khi Công ty mẹ gửi thông tin qua Zalo/Email cho tới khi hàng được cất vào Bin hoặc khu Quarantine. Trưởng kho chỉ phê duyệt để mở khóa putaway; chỉ khi putaway xong hệ thống mới cập nhật số tồn khả dụng.

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
│             │             │        │        │ Tạo RTV     │                │
│             │             │        │        │ Trả NCC─────►                │
│             │             │  [HÀNG ĐẠT]     │             │                │
│             │             │        ├────────► Phê duyệt   │                │
│             │             │        │        │ Phiếu nhập──►                │
│             │             │        │        │             │ Mở khóa        │
│             │             │        │        │             │ putaway        │
│             │             │                 │             │                │
└─────────────┴─────────────┴─────────────────┴─────────────┴────────────────┘
```

**Luồng trạng thái đơn nhập:**
`PENDING_RECEIPT` (Planner tạo) → `DRAFT` (Thủ kho kiểm đếm) → `QC_COMPLETED` (Thủ kho hoàn tất QC) → `APPROVED` (Trưởng kho duyệt, mở khóa putaway) / `RETURN_TO_SUPPLIER_PENDING` (Trưởng kho từ chối, chờ xe NCC đến lấy) → `RETURNED_TO_SUPPLIER` (Storekeeper xác nhận đã bàn giao NCC)

Trong Spec 003, xử lý Quarantine chỉ bao gồm RTV bằng nút "Trả NCC". Luồng tiêu hủy hàng lỗi được tách sang Spec 009.

---

### 2. Quy trình Xuất hàng (Issue Process)

Quy trình xử lý đơn xuất hàng bán cho Đại lý bao phủ toàn bộ Spec 004: Planner lập DO và hệ thống kiểm tra công nợ/tồn kho/phạm vi kho; Thủ kho lập picking plan FIFO; Nhân viên kho lấy hàng và QC outbound; Thủ kho duyệt chất lượng hoặc chọn replacement; Trưởng kho duyệt/từ chối xuất kho; Dispatcher lập chuyến xe; Tài xế giao hàng bằng POD + OTP; hệ thống tự động tạo invoice/công nợ sau khi giao thành công. Thanh toán, cấn trừ công nợ và chuyển `CLOSED` thuộc luồng tài chính riêng.

**Swimlane 004-A — tạo DO, picking, QC, duyệt kho:**

```
┌─────────────┬──────────────────────────────┬────────────────┬────────────────┬────────────────┐
│   PLANNER   │            SYSTEM            │    THỦ KHO     │ NHÂN VIÊN KHO  │   TRƯỞNG KHO   │
├─────────────┼──────────────────────────────┼────────────────┼────────────────┼────────────────┤
│ Lập Đơn xuất│                              │                │                │                │
│ (DO) ──────►│ Check công nợ, tồn kho,       │                │                │                │
│             │ phạm vi kho                  │                │                │                │
│             │ ├─ Không hợp lệ: chặn lỗi    │                │                │                │
│             │ └─ Hợp lệ: tạo DO [NEW],     │                │                │                │
│             │    reserve cấp tổng          │                │                │                │
│             │                              │◄──────────────►│                │                │
│             │                              │ Lập picking   │                │                │
│             │                              │ plan FIFO theo│                │                │
│             │                              │ batch/bin/zone│                │                │
│             │                              │ set [WAITING] │                │                │
│             │                              │                │◄──────────────►│                │
│             │                              │                │ Lấy hàng theo │                │
│             │                              │                │ plan, nhập QC │                │
│             │                              │                │ pass/fail theo│                │
│             │                              │                │ allocation    │                │
│             │                              │ Pass -> stage │                │                │
│             │                              │ Fail ->       │                │                │
│             │                              │ quarantine    │                │                │
│             │                              │ set [QC_WAIT] │                │                │
│             │                              │◄──────────────►│                │                │
│             │                              │ Duyệt chất   │                │                │
│             │                              │ lượng hoặc   │                │                │
│             │                              │ lập replace  │                │                │
│             │                              │ set [QC_DONE]│                │                │
│             │                              │                │                │◄──────────────►│
│             │                              │                │                │ Duyệt xuất    │
│             │                              │                │                │ hoặc reject   │
│             │                              │                │                │ set [APPROVED]│
└─────────────┴──────────────────────────────┴────────────────┴────────────────┴────────────────┘
```

**Swimlane 004-B — lập trip, giao hàng, POD/OTP, auto invoice:**

```
┌───────────────┬────────────────────────────────────────┬────────────────────────────┬────────────┐
│  DISPATCHER   │                 TÀI XẾ                 │           SYSTEM           │   ĐẠI LÝ   │
├───────────────┼────────────────────────────────────────┼────────────────────────────┼────────────┤
│ Gom DO        │                                        │                            │            │
│ [APPROVED],   │                                        │                            │            │
│ gán xe/tài xế │                                        │                            │            │
│ tạo trip      │                                        │                            │            │
│ [PLANNED] ───►│ Nhận chuyến, xác nhận nhận hàng/rời kho│                            │            │
│               │───────────────────────────────────────►│ Move staging -> IN_TRANSIT │            │
│               │                                        │ tạo delivery attempt       │            │
│               │                                        │ [IN_TRANSIT]               │            │
│               │                                        │                            │            │
│               │ Đến điểm giao, bàn giao hàng           │                            │            │
│               │ Upload goodsImage + signDocumentImage  │                            │            │
│               │ Yêu cầu OTP ──────────────────────────►│ Gửi OTP email              │ Nhận OTP   │
│               │ Nhập OTP Đại lý đọc ─────────────────►│ Verify OTP                 │ Đọc OTP    │
│               │                                        │                            │            │
│               │ [GIAO THÀNH CÔNG] ───────────────────►│ attempt [DELIVERED]        │            │
│               │                                        │ Trừ đúng DO khỏi IN_TRANSIT│            │
│               │                                        │ Auto invoice + receivable  │            │
│               │                                        │ DO [COMPLETED]             │            │
│               │                                        │                            │            │
│               │ [GIAO THẤT BẠI/TỪ CHỐI] ─────────────►│ attempt [FAILED]           │ Từ chối    │
│               │                                        │ Giữ hàng ở IN_TRANSIT      │            │
│               │                                        │ DO [RETURNED]              │            │
│               │                                        │                            │            │
│               │ Xác nhận xe quay về kho ─────────────►│ Trip [COMPLETED] khi mọi   │            │
│               │                                        │ DO [COMPLETED]/[RETURNED]  │            │
└───────────────┴────────────────────────────────────────┴────────────────────────────┴────────────┘
```

**Luồng trạng thái đơn xuất:**
`NEW` (Planner lập đơn; System check công nợ, tồn kho khả dụng và phạm vi kho đạt; hệ thống reserve cấp `warehouse_product_reservations`, chưa gán batch/bin/zone và chưa tăng `inventories.reserved_qty`) → `WAITING_PICKING` (Thủ kho lưu kế hoạch lấy hàng đầy đủ từ một hoặc nhiều batch/bin/zone FIFO; hệ thống chuyển reserve cấp warehouse/product sang concrete inventory reservation và snapshot `unit_price`) → `QC_PENDING_APPROVAL` (Nhân viên kho lấy hàng và nhập kết quả QC một lần theo item/allocation/batch/location/zone; hàng pass vào outbound staging, hàng fail vào Quarantine, tạo quarantine record/inventory adjustment và xóa reserve allocation fail; trạng thái này vẫn dùng khi pass chưa đủ để Thủ kho chọn replacement) → `QC_COMPLETED` (Thủ kho duyệt chất lượng khi đủ hàng đạt sau mọi replacement cần thiết) → `WAREHOUSE_APPROVED` (Trưởng kho phê duyệt xuất kho, DO đủ điều kiện cho Dispatcher lập trip) → `IN_TRANSIT` (Dispatcher lập trip `DELIVERY`, Tài xế xác nhận nhận hàng/rời kho; hệ thống chuyển QC-passed goods từ outbound staging sang kho ảo In-Transit và tạo delivery attempt hiện tại) → `COMPLETED` (Tài xế upload đủ `goodsImage` + `signDocumentImage`, Đại lý xác thực OTP email, delivery attempt chuyển `DELIVERED`; hệ thống trừ In-Transit cho đúng DO, tự động tạo invoice/công nợ theo giá snapshot và tăng `Dealer.current_balance`) / `RETURNED` (Đại lý từ chối hoặc giao thất bại; delivery attempt `FAILED`, hàng vẫn ở In-Transit đến luồng hoàn hàng riêng) → `DELIVERY_FAILED` (luồng hoàn hàng riêng xác nhận hàng returned đã quay lại kho). `CLOSED` chỉ xảy ra trong luồng tài chính/thanh toán riêng sau khi công nợ được tất toán/phê duyệt.

**Nhánh kiểm soát chính của Spec 004:** Warehouse Manager có thể hủy DO trước `WAREHOUSE_APPROVED` và hệ thống phải giải phóng reservation phù hợp; sau `QC_COMPLETED`, Trưởng kho có thể reject và hệ thống phải trả toàn bộ hàng pass ở outbound staging về bin gốc, giữ hàng fail trong Quarantine và kết thúc DO ở `REJECTED`; Dispatcher chỉ được update/cancel trip khi trip còn `PLANNED`; delivery confirmation chỉ áp dụng full DO, chỉ invoice đúng DO được xác nhận trong cùng trip, idempotent theo DO và rollback toàn bộ nếu tạo invoice/công nợ lỗi.

**Sửa picking plan sau khi đã có kết quả lấy/QC:** Storekeeper dùng cùng endpoint `PUT /api/v1/delivery-orders/{id}/picking-plan`; `allocations[]` là kế hoạch lấy hàng đầy đủ mới, `returnToBinRecords[]` chỉ bắt buộc cho allocation đã pick và bị remove/reduce. Allocation đã pick nhưng giữ nguyên không cần return; mỗi return ghi audit `PICKED_GOODS_RETURN_TO_BIN`. Khi QC fail cần hàng thay thế, Thủ kho lưu replacement plan từ `QC_PENDING_APPROVAL` và Delivery Order quay lại `WAITING_PICKING`.

**Định danh nguồn khi lấy hàng/QC:** Warehouse staff ghi nhận từng dòng theo `doItemId + allocationId + batchId + locationId + zoneId`; payload phải khớp batch/location/zone của allocation đã lập để tránh lấy nhầm lô hoặc nhầm bin/zone.

**Reject sau QC:** Warehouse reject phải trả toàn bộ hàng pass đang ở outbound staging về bin gốc; tổng `returnedQty` phải bằng tổng QC-passed còn ở staging. Hệ thống cộng lại available ở bin gốc, release reservation của hàng trả và ghi `PICKED_GOODS_RETURN_TO_BIN`.

---

### 3. Quy trình Điều chuyển Kho Nội bộ (Internal Transfer)

Quy trình điều phối hàng hóa giữa 3 kho vật lý Hải Phòng, Hà Nội, TP.HCM thông qua kho ảo trung chuyển `IN-TRANSIT` bằng xe nội bộ của Phúc Anh. Sprint 1 không có nghiệp vụ kho tự gợi ý hoặc tự quyết định điều chuyển; Planner nhập phiếu theo lệnh từ Công ty mẹ, bộ phận điều phối trung tâm, hoặc từ một yêu cầu điều chuyển do Trưởng kho đề xuất đã được CEO duyệt. Mã phiếu điều chuyển dùng `TRF-*`, mã chuyến điều chuyển dùng `TTR-*`, và luồng này tách riêng khỏi phiếu nhập NCC `RN-*`.

Trưởng kho của kho đang thiếu hàng có thể xem tồn kho khả dụng liên kho ở chế độ read-only để đề xuất điều chuyển. Yêu cầu điều chuyển này phải được CEO duyệt trước khi Planner kho nguồn hoặc Planner trung tâm chuyển thành `TRF-*`. CEO duyệt yêu cầu không giữ chỗ tồn, không sinh biến động inventory và không thay thế bước Trưởng kho nguồn duyệt phiếu `TRF-*`; reservation chỉ xảy ra khi phiếu `TRF-*` được Trưởng kho nguồn duyệt.

```
Truong kho kho thieu hang xem ton lien kho read-only, tao transfer request neu can; khi con DRAFT duoc sua hoac xoa mem thanh CANCELLED
    -> CEO duyet/tu choi transfer request
    -> Planner kho nguon/trung tam chuyen request da duyet thanh `TRF-*`
Planner nhap phieu `TRF-*` theo lenh ngoai hoac request da duoc CEO duyet
    -> Truong kho nguon duyet/tu choi va giu cho FIFO-eligible stock
    -> Dispatcher kho nguon lap chuyen `TTR-*`, gan xe va tai xe thuoc pham vi kho nguon, kiem tra trung lich, tai trong va the tich neu co cau hinh
    -> Thu kho nguon outbound QC bang mat/doi chieu phieu, bat buoc chon/chup anh xac nhan truoc khi bam QC, QC dat moi duoc ghi so gui/boc xep; QC that bai chi cho ha hang khoi xe
    -> Thu kho nguon chup/chon anh ban giao hang len xe truoc khi xac nhan handover cho tai xe
    -> Tai xe duoc gan xac nhan nhan hang va roi kho
        -> System tru ton kho nguon, giai phong reserved, cong kho ao `IN_TRANSIT`
    -> Tai xe ghi nhan den kho nhan va kho nhan bat buoc chon/chup anh truoc khi ghi nhan handover
    -> Cong nhan/Nhan vien kho dich blind count so nhan ban dau
    -> Thu kho dich kiem tra lai, chot QC, kiem tra bin capacity, chon vi tri nhap kho cho hang dat
    -> Truong kho dich xac nhan cuoi cung
        -> Khop + QC dat: tru `IN_TRANSIT`, cong ton kho dich, status `COMPLETED`
        -> Thieu: bat buoc ly do, tao incident/discrepancy + adjustment, status `COMPLETED_WITH_DISCREPANCY`
        -> QC loi/hu hong: phan loi vao Quarantine origin INTERNAL_TRANSFER, chi xu ly tieu huy theo Spec 009
        -> Gui nham SKU con nguyen: Storekeeper dich bao cao line expected/actual SKU + so luong + ly do/photo neu co, Truong kho dich duyet quay ve kho nguon, tai xe ghi return departure/source arrival/handover
        -> Trip qua han: chan receive o kho dich, vai tro co tham quyen kich hoat Return to Source voi ly do, kem photo neu co
        -> Nhan thua: chan regular inventory va ghi discrepancy hold/incident
```

**Swimlane điều chuyển nội bộ chuẩn:**

```
┌──────────────┬────────┬─────────┬──────────────┬────────────┬──────────────┬────────┬──────────────┬────────────────────┐
│ TRƯỞNG KHO   │  CEO   │ PLANNER │ TRƯỞNG KHO   │ DISPATCHER │ THỦ KHO      │ TÀI XẾ │ KHO NHẬN     │       SYSTEM       │
│ KHO THIẾU    │        │         │ KHO NGUỒN    │ KHO NGUỒN  │ KHO NGUỒN    │        │              │                    │
├──────────────┼────────┼─────────┼──────────────┼────────────┼──────────────┼────────┼──────────────┼────────────────────┤
│ Xem tồn liên │        │         │              │            │              │        │              │ Tính available,     │
│ kho read-only│        │         │              │            │              │        │              │ loại Quarantine/    │
│              │        │         │              │            │              │        │              │ In-Transit          │
│ Tạo TRQ      │        │         │              │            │              │        │              │                    │
│ [DRAFT]      │        │         │              │            │              │        │              │                    │
│ Sửa hoặc xóa │        │         │              │            │              │        │              │ Xóa = CANCELLED,   │
│ mềm khi DRAFT│        │         │              │            │              │        │              │ không delete DB    │
│ Submit ──────┼───────►│         │              │            │              │        │              │ TRQ [SUBMITTED]    │
│              │ Duyệt/ │         │              │            │              │        │              │                    │
│              │ từ chối│         │              │            │              │        │              │ Nếu duyệt:          │
│              │ ───────┼────────►│              │            │              │        │              │ TRQ [APPROVED],    │
│              │        │ Convert │              │            │              │        │              │ chưa reserve        │
│              │        │ TRQ hoặc│              │            │              │        │              │                    │
│              │        │ tạo TRF │              │            │              │        │              │ TRF [NEW]          │
│              │        │ thủ công│              │            │              │        │              │                    │
│              │        │─────────┼─────────────►│            │              │        │              │                    │
│              │        │         │ Duyệt/từ chối│            │              │        │              │ Nếu duyệt: reserve │
│              │        │         │ TRF [NEW]    │            │              │        │              │ FIFO eligible,      │
│              │        │         │──────────────┼───────────►│              │        │              │ TRF [APPROVED]     │
│              │        │         │              │ Gán hoặc   │              │        │              │ Tạo/gán TTR,       │
│              │        │         │              │ đổi trip   │              │        │              │ check scope,        │
│              │        │         │              │ trước depart│             │        │              │ overlap, tải/trọng │
│              │        │         │              │────────────┼─────────────►│        │              │                    │
│              │        │         │              │            │ Pick theo    │        │              │                    │
│              │        │         │              │            │ phiếu, không │        │              │                    │
│              │        │         │              │            │ Barcode/QR   │        │              │                    │
│              │        │         │              │            │ Outbound QC  │        │              │ Chọn/chụp ảnh      │
│              │        │         │              │            │ + ảnh        │        │              │ trước khi bấm QC;  │
│              │        │         │              │            │              │        │              │ QC pass mới ship   │
│              │        │         │              │            │ Ship đúng SL │        │              │ sent=planned       │
│              │        │         │              │            │ Handover ảnh ├──────►│              │ Handover photo     │
│              │        │         │              │            │ cho tài xế   │        │              │ required           │
│              │        │         │              │            │ cho tài xế   │ Depart │              │                    │
│              │        │         │              │            │              │───────►│              │ Trừ kho nguồn,     │
│              │        │         │              │            │              │        │              │ release reserved,  │
│              │        │         │              │            │              │        │              │ cộng IN_TRANSIT,   │
│              │        │         │              │            │              │        │              │ TRF [IN_TRANSIT]   │
│              │        │         │              │            │              │ Arrive │              │                    │
│              │        │         │              │            │              │───────┼─────────────► Ghi arrival        │
│              │        │         │              │            │              │        │ Handover     │ Chọn/chụp ảnh      │
│              │        │         │              │            │              │        │ nhận hàng ───► trước handover,    │
│              │        │         │              │            │              │        │              │ mở receive-count   │
│              │        │         │              │            │              │        │ Blind count  │                    │
│              │        │         │              │            │              │        │─────────────► Lưu received draft  │
│              │        │         │              │            │              │        │ Storekeeper  │                    │
│              │        │         │              │            │              │        │ check/QC/bin │ Check QC total,     │
│              │        │         │              │            │              │        │ capacity ───► quarantine/bin cap  │
│              │        │         │              │            │              │        │ Manager      │                    │
│              │        │         │              │            │              │        │ final receive│ Trừ IN_TRANSIT,    │
│              │        │         │              │            │              │        │─────────────► cộng kho/Quarantine │
│              │        │         │              │            │              │        │              │ hoặc discrepancy,  │
│              │        │         │              │            │              │        │              │ [COMPLETED/*]      │
└──────────────┴────────┴─────────┴──────────────┴────────────┴──────────────┴────────┴──────────────┴────────────────────┘
```

**Swimlane ngoại lệ chính:**

```
┌──────────────┬──────────────┬──────────────┬────────┬──────────────┬────────────────────┐
│  KHO NHẬN    │ TRƯỞNG KHO   │ TRƯỞNG KHO   │ TÀI XẾ │ KHO NGUỒN    │       SYSTEM       │
│              │ KHO NHẬN     │ KHO NGUỒN    │        │              │                    │
├──────────────┼──────────────┼──────────────┼────────┼──────────────┼────────────────────┤
│ Thiếu hàng   │              │              │        │              │ Tạo incident +     │
│ khi nhận ───►│ Final approve│              │        │              │ adjustment, không  │
│              │              │              │        │              │ tạo Quarantine     │
│              │              │              │        │              │                    │
│ Nhận thừa ──►│              │              │        │              │ Chặn regular       │
│              │              │              │        │              │ inventory, tạo hold│
│              │              │              │        │              │                    │
│ QC lỗi nặng ─┼─────────────►│              │        │              │ Chuyển Quarantine │
│              │ Từ chối &    │              │        │              │ origin INTERNAL_   │
│              │ cách ly      │              │        │              │ TRANSFER           │
│              │              │              │        │              │                    │
│ Wrong SKU ──►│ Duyệt/từ chối│              │ Return │ Source       │ Giữ IN_TRANSIT,    │
│ report line  │ quay về      │              │ depart │ arrival/     │ không tạo TRF mới, │
│ expected/    │              │              │ +      │ handover +   │ source receive     │
│ actual SKU   │              │              │ arrive │ receive      │                    │
│              │              │              │        │              │                    │
│ Trip overdue │              │ Return to    │ Return │ Source       │ Block receive      │
│              │              │ Source + lý  │ depart │ arrival/     │ kho đích, yêu cầu  │
│              │              │ do/photo nếu │ +      │ handover +   │ lý do, giữ audit   │
│              │              │ có           │ arrive │ receive      │                    │
└──────────────┴──────────────┴──────────────┴────────┴──────────────┴────────────────────┘
```

**Luồng trạng thái phiếu điều chuyển:**
`NEW` (Planner nhap phieu nhieu dong hang theo lenh tu Cong ty me/bo phan dieu phoi trung tam hoac transfer request da duoc CEO duyet; Planner duoc sua dong hang hoac huy phieu khi con `NEW`) -> `APPROVED` (Truong kho nguon duyet va giu cho hang ngay) hoac `REJECTED` (Truong kho nguon tu choi va bat buoc nhap ly do; phieu rejected khong duoc sua/gui lai, phai tao phieu moi neu can tiep tuc) -> `IN_TRANSIT` (Dispatcher da lap chuyen xe rieng, Thu kho nguon ghi so gui, Tai xe duoc gan xac nhan roi kho; he thong dich chuyen ton kho vao kho trung chuyen `IN_TRANSIT`) -> `COMPLETED` (Cong nhan kho dich nhap so nhan, Thu kho dich kiem tra + QC + chon vi tri, Truong kho dich xac nhan cuoi cung va khop so luong) / `COMPLETED_WITH_DISCREPANCY` (Nhan thieu, tao phieu dieu chinh bu tru va log audit) / `QUARANTINED` (hang dieu chuyen bi tu choi toan bo do hu hong/QC loi va dua vao Quarantine). Sau `APPROVED` khong cho sua header/dong hang; chi Truong kho nguon/manager duoc huy truoc khi `IN_TRANSIT` va phai giai phong reserved quantity. Neu `received_qty > sent_qty` he thong chan xac nhan; neu QC loi thi phan loi vao Quarantine va khong tinh available. Hang thieu khong tao Quarantine/disposal candidate vi khong ton tai vat ly. Khong ho tro huy phieu sau khi da `IN_TRANSIT`.

**Quy tắc chuyến xe điều chuyển:**

- Mỗi Phiếu điều chuyển gắn đúng một chuyến xe nội bộ riêng (`trips.trip_type = TRANSFER`).
- Không gom nhiều Phiếu điều chuyển vào một chuyến xe trong Sprint 1.
- Dispatcher chỉ được lập chuyến cho các phiếu có kho nguồn thuộc phạm vi kho mình phụ trách.
- Danh sách tài xế hợp lệ chỉ gồm tài xế có thể hoạt động tại kho nguồn của phiếu điều chuyển.
- Dispatcher phải tính tải trọng/thể tích từ dòng hàng, kiểm tra xe/tài xế không bị trùng lịch; kiểm tra tải trọng theo cân nặng, và kiểm tra thể tích khi xe có `max_volume_m3`.
- Chỉ được đổi xe/tài xế/lịch trước khi departure; sau departure trip bị khóa.
- Tài xế phải xác nhận đã nhận hàng và xe rời kho trước khi hệ thống chuyển tồn sang `IN_TRANSIT`.
- Tài xế phải ghi nhận arrival và receiving handover trước khi kho nhận được receive-count.

**Ngoại lệ điều chuyển và Quarantine:**

- Hàng điều chuyển fail QC hoặc hư hỏng đi vào Quarantine với origin `INTERNAL_TRANSFER`, giữ traceability tới transfer/transfer item/trip/vehicle/driver và chỉ đi luồng tiêu hủy Spec 009; không tạo supplier RTV hoặc supplier Debit Note.
- Hàng thiếu khi nhận là discrepancy số lượng: tạo incident/discrepancy + adjustment/audit `TRANSFER_DISCREPANCY`, tính giá trị nhập kho đích chỉ theo số lượng thực nhận và không tạo invoice/receivable/payable/Debit Note.
- Nhận thừa bị chặn khỏi regular inventory và phải ghi discrepancy hold/incident cho phần hàng vật lý thừa.
- Gửi nhầm SKU nhưng hàng còn nguyên được xử lý bằng Return to Source: Storekeeper kho đích báo cáo line-level expected SKU, actual SKU, quantity, reason và photo refs nếu có; Trưởng kho đích duyệt, cùng tài xế/xe quay về kho nguồn, hàng vẫn nằm trong `IN_TRANSIT` cho tới khi kho nguồn count/check/QC/final receive.
- Nếu chuyến quá hạn khi phiếu còn `IN_TRANSIT`, hệ thống đánh dấu overdue, chặn receive-count/receive-check ở kho đích và yêu cầu WAREHOUSE_MANAGER kho nguồn, ADMIN, CEO hoặc PLANNER kích hoạt Return to Source với lý do, kèm photo refs nếu có.
- Hàng đạt QC khi nhập vào kho đích hoặc kho nguồn sau Return to Source phải chọn Bin hợp lệ, không phải quarantine bin, và phải kiểm tra bin capacity trước khi cộng tồn.
- Pick/outbound QC/load handover trong điều chuyển không dùng Barcode/QR; xác nhận bằng chọn line phiếu, nhập/xác nhận số lượng và ảnh chụp.
- Các bước có ảnh trong UI phải dùng nút chọn file/chọn ảnh trên điện thoại/máy tính hoặc chụp trực tiếp bằng camera; không nhập link ảnh thủ công. UI nén ảnh camera lớn, upload multipart trước, rồi các action chỉ gửi `photoRef` ngắn; không gửi base64/data URL trong JSON. Nút QC/xác nhận bàn giao/POD chỉ được bật sau khi đã có ảnh.
- Transfer/request/trip/resource/inventory mutations phải có version/concurrency guard; GET/list/detail không được mutate trạng thái nghiệp vụ.
- Audit transfer phải đủ header, items, allocation, QC, wrong-SKU lines, trip/resource state và inventory movement để tái dựng nghiệp vụ.
- Flyway migrations đã apply không được sửa/rename/xóa; schema fix phải đi qua migration mới.

---

### 4. Quy trình Giao hàng (Delivery Process)

Quy trình điều phối chuyến xe, vận chuyển bằng xe nội bộ của công ty và cập nhật trạng thái đơn hàng kèm ảnh hàng bàn giao, ảnh chữ ký/biên nhận POD của Đại lý, và OTP email xác nhận trên thiết bị di động của Tài xế. Mỗi bản ghi `deliveries` đại diện cho một lần giao vật lý của một Delivery Order; nếu giao lại sau thất bại thì tạo `deliveries` record mới với `attempt_number` kế tiếp. Attempt hiện tại là bản ghi mới nhất theo `trip_id + do_id + driver_id` chưa ở trạng thái terminal. Raw OTP không được lưu DB; hệ thống chỉ lưu hash/verifier, email nhận, hạn hiệu lực, số lần thử và trạng thái trong bảng `delivery_otp_attempts`.

```
┌───────────────┬───────────────────────────────────────────────┬────────────┐
│  DISPATCHER   │                    TÀI XẾ                     │   SYSTEM   │
├───────────────┼───────────────────────────────────────────────┼────────────┤
│               │                                               │            │
│ Gom đơn [Ready│                                               │            │
│ to Ship], gán │                                               │            │
│ xe nội bộ ───►│ Nhận chuyến qua Smartphone,                   │            │
│               │ xác nhận bốc xếp hàng, rời kho ──────────────►│ Trừ kho xuất,│
│               │                                               │ cộng In-Transit│
│               │                                               │ set trạng  │
│               │                                               │ thái đơn   │
│               │                                               │ [IN_TRANSIT]│
│               │                                               │            │
│               │ Đến điểm giao, bàn giao hàng cho Đại lý       │            │
│               │        │                                      │            │
│               │  [GIAO THÀNH CÔNG]                            │            │
│               │        ├─────────────────────────────────────►│ Gửi OTP email,│
│               │        │ Chụp ảnh hàng, ảnh chữ ký/biên nhận  │ xác thực OTP│
│               │        │ Đại lý đọc OTP cho Tài xế nhập       │ Trừ In-Transit,│
│               │        │                                      │ attempt    │
│               │        │                                      │ [DELIVERED]│
│               │        │                                      │ DO         │
│               │        │                                      │ [COMPLETED]│
│               │  [GIAO THẤT BẠI]                              │            │
│               │        ├─────────────────────────────────────►│ Ghi lý do, │
│               │        │ Ghi lý do (vắng, từ chối, sai...)     │ giữ hàng ở │
│               │        │                                      │ In-Transit │
│               │        │                                      │ set trạng  │
│               │        │                                      │ thái đơn   │
│               │        │                                      │ [Returned] │
│               │                                               │            │
└───────────────┴───────────────────────────────────────────────┴────────────┘
```

**Luồng trạng thái đơn giao:**
`WAREHOUSE_APPROVED` → `IN_TRANSIT` (Dispatcher lập trip cùng kho, chọn xe/tài xế thuộc kho và kiểm tra tải trọng; Tài xế nhận hàng lên xe; hệ thống chuyển hàng từ outbound staging sang kho ảo In-Transit và tạo delivery attempt hiện tại) → `COMPLETED` (Tài xế upload `goodsImage` và `signDocumentImage` vào attempt hiện tại, Đại lý xác thực OTP email, delivery attempt chuyển `DELIVERED`, hệ thống bắt buộc giao đủ DO, chỉ trừ kho ảo In-Transit của DO đó và tự động tạo invoice/công nợ cho DO đó theo giá snapshot trên phiếu xuất tại thời điểm Thủ kho soạn/lập picking plan) / `RETURNED` (Đại lý không nhận hoặc giao thất bại; delivery attempt hiện tại đóng `FAILED`, ghi lý do; hàng vẫn ở kho ảo In-Transit cho tới khi luồng hoàn hàng riêng tiếp nhận)

**Trip outbound:** Mỗi trip outbound có `trip_type = DELIVERY`, phải có ít nhất một DO `WAREHOUSE_APPROVED` cùng kho; Dispatcher, xe và tài xế phải thuộc kho đó. Trip `PLANNED` được sửa xe/tài xế/ngày/stop order hoặc danh sách DO trước khi depart; `deliveryOrders[]` khi update là danh sách cuối cùng sau chỉnh sửa và thay thế danh sách cũ. Hủy trip giữ DO ở `WAREHOUSE_APPROVED`, giữ lịch sử xe/tài xế trên trip, nhưng giải phóng xe/tài xế khỏi active assignment. Kiểm tra tải trọng luôn áp dụng theo cân nặng; thể tích chỉ kiểm tra nếu xe có `max_volume_m3`. Khi depart, hệ thống chuyển hàng từ outbound staging sang kho ảo In-Transit và tạo delivery attempt `IN_TRANSIT`; Sprint 1 không dùng `OUT_FOR_DELIVERY`. Trip chỉ `COMPLETED` khi Tài xế xác nhận xe đã quay lại kho và toàn bộ DO trong trip đã `COMPLETED` hoặc `RETURNED`.

**Màn tài xế chung:** Mobile driver entry point phải là `Vận hành / Chuyến xe` với tiêu đề `Chuyến xe của tôi`, không dùng wording chỉ dành cho giao đại lý. Danh sách hiển thị cả `trip_type = DELIVERY` (`TRIP-*`, nhãn `Giao đại lý`) và `trip_type = TRANSFER` (`TTR-*`, nhãn `Điều chuyển nội bộ`) được gán cho driver profile hiện tại. Có 3 filter `Tất cả`, `Nội bộ`, `Đại lý`; `Nội bộ` chỉ hiện transfer trip và card hiển thị tuyến kho nguồn → kho đích/số dòng hàng, `Đại lý` chỉ hiện delivery trip và card hiển thị số điểm giao đại lý. POD/OTP/từ chối đại lý/auto invoice chỉ xuất hiện trong detail `DELIVERY`; transfer detail đi theo luồng depart/arrive/handover của Spec 005.

**Lưu trữ OTP giao hàng:** Backend sinh OTP ngẫu nhiên 6 chữ số. Mỗi delivery attempt chỉ có một bản ghi `delivery_otp_attempts` liên kết với `deliveries`; chỉ lưu hash/verifier của OTP, `recipient_email`, `created_at`, `expires_at`, `consumed_at`, `attempt_count`, và `status`. OTP có hiệu lực 5 phút. Nếu OTP còn hạn và Tài xế yêu cầu gửi lại, hệ thống trả lỗi và không ghi đè mã cũ. Nếu OTP quá hạn và Tài xế yêu cầu gửi lại, hệ thống dùng `UPDATE` ghi đè OTP hiện tại của delivery attempt bằng mã mới và reset thời gian/số lần thử/trạng thái. Nếu nhập sai OTP 3 lần, OTP bị khóa; Admin phải nhập lý do reset, hệ thống đánh dấu row hiện tại `EXPIRED`, reset `attempt_count = 0`, ghi audit before/after, rồi Tài xế mới được yêu cầu mã mới trên cùng row. Khi xác thực thành công, OTP chuyển `VERIFIED`, ghi `consumed_at`, và không được dùng lại. Bảng `deliveries` chỉ lưu kết quả xác thực cuối cùng như `otp_verified_at`, không lưu raw OTP.

**Ý nghĩa trạng thái DO sau giao hàng:** `COMPLETED` nghĩa là Đại lý đã nhận hàng, POD + OTP hợp lệ, delivery attempt đã `DELIVERED`, và hệ thống đã tự động tạo invoice/công nợ; thông báo kế toán thuộc luồng riêng. `CLOSED` thuộc luồng tài chính/thanh toán riêng sau khi công nợ được tất toán hoặc khóa theo kỳ kế toán.

**Phân quyền kho trong outbound:** Mọi API outbound phải check cả role và warehouse assignment. Planner, Thủ kho, QC, Trưởng kho, Dispatcher và Kế toán chỉ thao tác dữ liệu thuộc kho được gán; Tài xế chỉ thấy chuyến/attempt được gán cho driver profile của mình. CEO/Admin có thể xem liên kho nhưng mọi mutation vẫn phải ghi audit log.

---

### 5. Chu kỳ Tài chính & Kiểm soát Công nợ (Finance & Credit Cycle)

Chu kỳ lập Hóa đơn bán hàng, theo dõi hạn mức công nợ (Credit Limit), chặn nợ quá hạn và thực hiện chốt sổ kế toán tháng (Monthly Closing).

```
┌─────────────────────────────────┬────────────────────────────────┬─────────┐
│          KẾ TOÁN VIÊN           │             SYSTEM             │ KTT/CEO │
├─────────────────────────────────┼────────────────────────────────┼─────────┤
│                                 │                                │         │
│ Theo dõi invoice auto-created       │                            │         │
│ giá snapshot lúc soạn picking,  │ Cộng dồn current_balance,      │         │
│ issue_date = ngày local,        │                            │         │
│ due_date = issue_date + 30d ───►│                            │         │
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

- Tất cả chênh lệch kiểm kê và phiếu xuất hủy hàng lỗi đều do Trưởng kho phê duyệt trực tiếp trên hệ thống, không phân cấp theo giá trị.

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

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **Manager-warehouse-sdd** (11711 symbols, 29938 relationships, 300 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> If any GitNexus tool warns the index is stale, run `npx gitnexus analyze` in terminal first.

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `gitnexus_impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `gitnexus_detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `gitnexus_query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `gitnexus_context({name: "symbolName"})`.

## Never Do

- NEVER edit a function, class, or method without first running `gitnexus_impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `gitnexus_rename` which understands the call graph.
- NEVER commit changes without running `gitnexus_detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/Manager-warehouse-sdd/context` | Codebase overview, check index freshness |
| `gitnexus://repo/Manager-warehouse-sdd/clusters` | All functional areas |
| `gitnexus://repo/Manager-warehouse-sdd/processes` | All execution flows |
| `gitnexus://repo/Manager-warehouse-sdd/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->
