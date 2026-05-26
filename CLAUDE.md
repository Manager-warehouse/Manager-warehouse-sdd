# CLAUDE.md — Warehouse Management System (WMS) v1.0
## Hệ Thống Quản Lý Kho cho doanh nghiệp thương mại

---

## TL;DR (Đọc trước — 60 giây)

> **Đây là hệ thống quản lý kho hàng (Warehouse Management System)**
> 
> **Backend**: Spring Boot 3.4.5 + Java 21 + PostgreSQL 18 + JPA/Hibernate
> **Frontend**: React 18 + TypeScript + Tailwind CSS 3.x
> **Auth**: JWT + bcrypt (cost factor 12)
> **Integration**: Message queue cho Accounting events (Kafka/RabbitMQ)
> 
> **3 warehouses**: Hải Phòng, Hà Nội, Hồ Chí Minh
> **Scale**: 1000+ products, 50+ dealers, 1000+ transactions/month

### Đọc trước
1. `AGENTS.md` → Project context đầy đủ (Tech stack, forbidden patterns, domain model)
2. `CONSTITUTION.md` → Development principles và team agreements
3. File này → Workflow, patterns, và conventions

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
│  │  /api/warehouse-stock  /api/batch-management  /api/receipt     │   │
│  │  /api/issue  /api/transfer  /api/delivery  /api/sale-order      │   │
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
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   PostgreSQL    │  │  Message Queue   │  │   File Storage  │
│   (Primary DB)  │  │ (Kafka/RabbitMQ) │  │   (/uploads)    │
└─────────────────┘  │   → Accounting   │  └─────────────────┘
                     └─────────────────┘
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
│           Entity Layer                   │  @Entity
│   - Database table mapping              │  - JPA annotations
│   - Relationships                       │  
└─────────────────────────────────────────┘
```

### Repository Structure
```
/Users/haison/Documents/GitHub/Manager-warehouse-sdd
├── .agents/             # Agent and skill definitions
├── .git/                # Git metadata
├── .specify/            # Spec generation workspace
├── AGENTS.md            # Agent policy and rules
├── CLAUDE.md            # Project overview, workflow, and conventions
├── CONSTITUTION.md      # Development principles and agreements
├── README.md            # Project summary and user stories
└── test/                # Test plans and guidance
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

### ADR-006: Message Queue cho Accounting Events
**Quyết định**: Gửi events tới Accounting qua Kafka/RabbitMQ
**Lý do**: Async processing, decouple systems, không block warehouse ops
**Trade-off**: Complexity cao hơn sync HTTP calls
**Status**: ✅ Approved (Pending integration specs)

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

## FILE STRUCTURE

### Backend (Spring Boot)
```
backend/
├── src/main/java/com/wms/
│   ├── controller/         # REST controllers
│   │   └── WarehouseController.java
│   ├── service/           # Business logic
│   │   └── WarehouseService.java
│   ├── repository/         # JPA repositories
│   │   └── WarehouseRepository.java
│   ├── entity/             # JPA entities
│   │   └── Warehouse.java
│   ├── dto/               # Data transfer objects
│   │   ├── WarehouseDTO.java
│   │   └── CreateWarehouseRequest.java
│   ├── config/            # Configuration classes
│   │   ├── SecurityConfig.java
│   │   └── JpaConfig.java
│   ├── exception/         # Custom exceptions
│   │   ├── GlobalExceptionHandler.java
│   │   └── InsufficientStockException.java
│   ├── event/             # Domain events
│   │   └── WarehouseEvent.java
│   └── util/              # Utilities
│       └── FEFOSelector.java
├── src/main/resources/
│   ├── db/migration/      # Flyway migrations
│   │   └── V1__init_schema.sql
│   └── application.yml
├── src/test/java/         # Unit tests (mirrors main structure)
└── CLAUDE.md              # Backend-specific patterns
```

### Frontend (React + TypeScript)
```
frontend/
├── src/
│   ├── components/        # React components (PascalCase)
│   │   ├── warehouse/
│   │   │   ├── WarehouseList.tsx
│   │   │   └── WarehouseCard.tsx
│   │   └── common/
│   │       └── Button.tsx
│   ├── pages/             # Page components
│   │   └── WarehousePage.tsx
│   ├── hooks/             # Custom hooks (camelCase)
│   │   └── useWarehouse.ts
│   ├── services/          # API calls
│   │   └── warehouseApi.ts
│   ├── stores/            # State management (Zustand/Redux)
│   ├── utils/             # Utility functions
│   │   └── formatCurrency.ts
│   └── types/             # TypeScript types
│       └── warehouse.types.ts
├── public/
└── CLAUDE.md              # Frontend-specific patterns
```

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

| Phase | Command | Purpose |
|-------|---------|---------|
| **Define** | `speckit-specify` | Create/update feature specification |
| **Plan** | `speckit-plan` | Decompose into tasks with acceptance criteria |
| **Build** | `speckit-implement` | Execute tasks incrementally |
| **Verify** | `tdd` skill | TDD cycle (RED-GREEN-REFACTOR) |
| **Review** | `code-review` skill | Five-axis code review |
| **Deploy** | `deploy` skill | Build, test, deploy |

### Supporting Commands

| Command | Use When |
|---------|----------|
| `/debug` | Systematic error diagnosis |
| `/simplify` | Reduce complexity without changing behavior |
| `speckit-analyze` | Check spec consistency |
| `speckit-git-validate` | Validate branch naming |

---

## RULES & GUIDELINES

### ✅ ALWAYS DO

| Rule | Description |
|------|-------------|
| **Input validation** | Use Jakarta Validation annotations |
| **Audit logging** | Log all warehouse operations (who, when, what) |
| **Inventory check** | Always check stock BEFORE issue |
| **Structured logging** | Use SLF4J (no console.log/system.out) |
| **Unit tests** | Min 80% coverage for services |
| **API docs** | Document in OpenAPI/Swagger |
| **Error handling** | Proper HTTP status codes |
| **Comments** | Explain WHY not WHAT |

### ❌ NEVER DO

| Rule | Description |
|------|-------------|
| **No secrets** | Never store passwords/keys in plain text |
| **No raw SQL** | Always use JPA/Hibernate |
| **No negative inventory** | Prevent under all circumstances |
| **No skip QC** | Always check before warehouse receipt |
| **No skip audit** | Log all warehouse operations |
| **No TODO left** | Remove before merge |
| **No deprecated libs** | Without team approval |

### Code Quality

| Metric | Limit |
|--------|-------|
| Max function length | 40 lines |
| Max file length | 300 lines |
| Min test coverage | 80% (services) |

---

## NAMING CONVENTIONS

### Java (Backend)

| Type | Convention | Example |
|------|------------|---------|
| Classes | PascalCase | `WarehouseService.java` |
| Packages | lowercase | `com.wms.service` |
| Tables | snake_case | `warehouse_staff` |
| Constants | UPPER_SNAKE | `MAX_BATCH_QUANTITY` |
| Methods | camelCase | `findByWarehouseId()` |

### React (Frontend)

| Type | Convention | Example |
|------|------------|---------|
| Components | PascalCase | `WarehouseDashboard.tsx` |
| Hooks | camelCase | `useInventory.ts` |
| Utils | camelCase | `formatCurrency.ts` |
| Types | PascalCase | `WarehouseType.ts` |

### API Routes

| Type | Convention | Example |
|------|------------|---------|
| Endpoints | kebab-case | `/api/warehouse-stock` |
| HTTP methods | lowercase | `GET /api/batch-management` |

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

| Tool | Purpose |
|------|---------|
| `gitnexus_query` | Find execution flows for a concept |
| `gitnexus_impact` | Blast radius analysis |
| `gitnexus_context` | Full symbol context (callers, callees) |
| `gitnexus_detect_changes` | Map git changes to affected flows |

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

| Task | Tool | Why |
|------|------|-----|
| "Find all FEFO implementations" | **Semble** | Semantic search across codebase |
| "What breaks if I change X?" | **GitNexus** | Call graph + impact analysis |
| "Show me code similar to BatchService" | **Semble** | Similarity detection |
| "Who calls this method?" | **GitNexus** | Relationship graph |
| "Trace receipt flow end-to-end" | **GitNexus** | Execution flow analysis |
| "Find validation patterns" | **Semble** | Pattern search across files |

### MCP Tools Available

| Tool | Purpose | Example |
|------|---------|---------|
| `semble.search()` | Semantic code search | `semble.search("inventory quantity validation")` |
| `semble.find_related()` | Find similar code | `semble.find_related("BatchService.selectFEFO")` |

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

| Scenario | Use CLI | Use MCP (via AI) |
|----------|---------|------------------|
| Quick manual search | ✅ `semble search "query" .` | ❌ |
| Exploring codebase manually | ✅ Direct terminal | ❌ |
| AI-driven workflow | ❌ | ✅ AI calls MCP tools |
| Integrated with GitNexus | ❌ | ✅ AI orchestrates both |
| Part of automated task | ❌ | ✅ AI decides when to use |

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

| Tool | Purpose | Example |
|------|---------|---------|
| `speckit_specify` | Create/update feature specification | Define new feature requirements |
| `speckit_plan` | Decompose spec into tasks | Break down feature into actionable steps |
| `speckit_implement` | Execute tasks incrementally | Build feature step-by-step |
| `speckit_analyze` | Check spec consistency | Validate specification completeness |
| `speckit_git_validate` | Validate branch naming | Ensure git conventions |

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
/specs/
├── 001-warehouse-management-system/
│   ├── spec.md                    # Main specification
│   ├── plan.md                    # Task breakdown
│   └── implementation-log.md      # Progress tracking
├── 002-fefo-batch-selection/
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
└── Locations (aisle-rack-shelf)

Product (1000+ items)
├── SKU, barcode, name, unit
└── Prices (cost, retail, dealer)

Batch (Lô hàng - tied to ONE grade)
├── batchNumber, receivedDate, expDate
├── grade (A/B/C)
└── quantity

Inventory (tồn kho)
├── warehouse + product + batch + location
└── quantity (NEVER negative)

Receipt (Phiếu nhập kho)
├── receiptNumber, type (purchase/return)
├── warehouse, supplier
└── items (product + quantity)

Issue (Phiếu xuất kho)
├── issueNumber, type (sale/delivery/adjustment)
├── warehouse, customer/dealer
└── items

Transfer (Điều chuyển)
├── source → destination warehouses
├── status (pending/in_transit/completed)
└── In-Transit virtual location

SaleOrder → Issue → Delivery
Dealer (50+ accounts)
WarehouseStaff
```

### Key Business Rules

| Rule | Implementation |
|------|----------------|
| No negative inventory | `@Column(check = "quantity >= 0")` + validation |
| Single grade per batch | `grade` is immutable after creation |
| FEFO for expiring products | `FEFOSelector` picks batch by expDate ASC |
| FIFO for non-expiring | `FIFOSelector` picks by receivedDate ASC |
| Quarantine excluded | WHERE clause filters `zone != 'QUARANTINE'` |
| In-Transit tracking | Virtual warehouse `IN_TRANSIT` for transfers |

### Current Sprint

- **Sprint 1**: Core Warehouse Operations
- **Focus**: Inventory, Receipt, Issue, Transfer
- **Active specs**: `specs/001-warehouse-management-system/spec.md`
- **Pending**: Accounting/HRM/Sale API integration specs

---

## MCP ARCHITECTURE OVERVIEW

### Data Flow
```
User Input → AI Model → MCP Client → MCP Server → External Service
                    ↑                     ↓
                    └───── JSON-RPC ─────┘
```

### Security Principles

| Principle | Application |
|-----------|-------------|
| **Least Privilege** | Only request necessary scopes |
| **Token Rotation** | Rotate MCP tokens regularly |
| **Audit Logging** | Log all MCP tool usage |
| **Validation** | Validate all inputs before use |

---

<!-- Claude Code auto-appends entries below when working -->
<!-- Last updated: 2026-05-25 -->

---

## SWIMLANE DIAGRAMS

### Business Process Swimlanes

#### Receipt Process (Phiếu nhập kho)
```
┌─────────────────────────────────────────────────────────────────────────────┐
│ RECEIPT PROCESS SWIMLANES                                                   │
├───────────────┬───────────────┬───────────────┬───────────────┬────────────┤
│    STAFF      │    SYSTEM     │    QC TEAM    │   ACCOUNTING   │  SUPPLIER  │
├───────────────┼───────────────┼───────────────┼───────────────┼────────────┤
│               │               │               │               │            │
│ Create receipt│               │               │               │            │
│ ─────────────►│               │               │               │            │
│               │               │               │               │            │
│ Enter SKU/code│               │               │               │            │
│ ─────────────►│               │               │               │            │
│               │               │               │               │            │
│ Submit for QC │               │               │               │            │
│ ─────────────►│───────────────│───────────────│               │            │
│               │               │               │               │            │
│               │               │ QC Check      │               │            │
│               │               │ ─────────────►│               │            │
│               │               │               │               │            │
│ Pass: Stock   │ Update inventory               │               │            │
│ Fail: Quarantine              │               │               │            │
│               │               │               │               │            │
│               │               │               │ Send invoice  │            │
│               │               │               │ ─────────────►│            │
│               │               │               │               │            │
└───────────────┴───────────────┴───────────────┴───────────────┴────────────┘
```

#### Issue Process (Phiếu xuất kho)
```
┌─────────────────────────────────────────────────────────────────────────────┐
│ ISSUE PROCESS SWIMLANES                                                     │
├───────────────┬───────────────┬───────────────┬───────────────┬────────────┤
│    STAFF      │    SYSTEM     │   PICKING     │   DELIVERY     │  CUSTOMER  │
├───────────────┼───────────────┼───────────────┼───────────────┼────────────┤
│               │               │               │               │            │
│ Create issue  │               │               │               │            │
│ ─────────────►│               │               │               │            │
│               │               │               │               │            │
│               │ FEFO Selection│               │               │            │
│               │ ─────────────►│               │               │            │
│               │               │               │               │            │
│               │               │ Pick items    │               │            │
│               │               │ ─────────────►│               │            │
│               │               │               │               │            │
│               │               │ Confirm pick  │               │            │
│               │◄──────────────│               │               │            │
│               │               │               │               │            │
│               │ Update stock  │               │               │            │
│               │ ◄─────────────│               │               │            │
│               │               │               │               │            │
│               │               │               │ Dispatch      │            │
│               │               │               │ ─────────────►│            │
│               │               │               │               │            │
└───────────────┴───────────────┴───────────────┴───────────────┴────────────┘
```

#### Transfer Process (Điều chuyển kho)
```
┌─────────────────────────────────────────────────────────────────────────────┐
│ TRANSFER PROCESS SWIMLANES                                                  │
├───────────────┬───────────────┬───────────────┬───────────────┬────────────┤
│   SOURCE WH    │    SYSTEM     │    TRANSIT    │  DEST WH      │   DRIVER  │
├───────────────┼───────────────┼───────────────┼───────────────┼────────────┤
│               │               │               │               │            │
│ Create transfer              │               │               │            │
│ ─────────────►│               │               │               │            │
│               │               │               │               │            │
│ Pick & pack   │               │               │               │            │
│ ─────────────►│               │               │               │            │
│               │               │               │               │            │
│               │ Move to IN_TRANSIT             │               │            │
│               │ ◄─────────────│               │               │            │
│               │               │               │               │            │
│               │               │ In Transit     │               │            │
│               │               │ ◄──────────────│               │            │
│               │               │               │               │            │
│               │               │               │ Confirm receipt              │
│               │◄──────────────│               │               │            │
│               │               │               │               │            │
│               │ Update dest stock             │               │            │
│               │               │               │               │            │
└───────────────┴───────────────┴───────────────┴───────────────┴────────────┘
```

#### Sale Order Process (Đơn hàng Sale)
```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ SALE ORDER PROCESS SWIMLANES                                                        │
├──────────────┬──────────────┬──────────────┬──────────────┬──────────────┬──────────┤
│   DEALER     │    SALE      │    SYSTEM    │  ACCOUNTING  │  WAREHOUSE   │  DELIVERY│
├──────────────┼──────────────┼──────────────┼──────────────┼──────────────┼──────────┤
│              │              │              │              │              │              │
│ Contact Sale │              │              │              │              │              │
│ ────────────►│              │              │              │              │              │
│              │              │              │              │              │              │
│              │ Create order │              │              │              │              │
│              │ ─────────────►│              │              │              │              │
│              │              │              │              │              │              │
│              │              │ Status: CHỜ_KETOAN_DUYET │              │          │
│              │◄─────────────│              │              │              │              │
│              │              │              │              │              │              │
│              │              │              │ Review order│              │          │
│              │              │              │ ────────────►│              │              │
│              │              │              │              │              │              │
│              │              │              │ Upload contract photo      │          │
│              │              │              │ ◄────────────│              │              │
│              │              │              │              │              │              │
│              │              │ Approve: Status=CHỜ_KHO_DUYET          │          │
│              │              │ ◄────────────│              │              │              │
│              │              │              │              │              │          │
│              │              │              │              │ Review order│          │
│              │              │              │              │ ────────────►│          │
│              │              │              │              │              │          │
│              │              │              │              │ Approve: DANG_CHUAN_BI     │
│              │              │              │              │ ◄────────────│          │
│              │              │              │              │              │          │
│              │              │              │              │ Prepare goods             │
│              │              │              │              │ ────────────►│          │
│              │              │              │              │              │          │
│              │              │              │              │ Confirm shipped           │
│              │              │              │◄─────────────│              │          │
│              │              │              │              │              │          │
│              │              │ Sync to Accounting (công nợ)│              │          │
│              │              │ ────────────►│              │              │          │
│              │              │              │              │              │          │
│              │ Notify dealer│              │              │              │          │
│              │ ◄────────────│              │              │              │          │
│              │              │              │              │              │          │
└──────────────┴──────────────┴──────────────┴──────────────┴──────────────┴──────────┘

Status Flow:
CHỜ_KETOAN_DUYET → CHỜ_KHO_DUYET → DA_DUYET → DANG_CHUAN_BI → DA_XUAT_KHO → HOAN_THANH/DA_HUY
```

#### Delivery Process (Vận chuyển & Giao hàng)
```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ DELIVERY PROCESS SWIMLANES                                                         │
├──────────────┬──────────────┬──────────────┬──────────────┬──────────────┬──────────┤
│   WAREHOUSE  │    SYSTEM    │   FLEET MGMT │    DRIVER    │   CUSTOMER   │ ACCTNG   │
├──────────────┼──────────────┼──────────────┼──────────────┼──────────────┼──────────┤
│              │              │              │              │              │          │
│ Create delivery              │              │              │              │          │
│ ────────────►│              │              │              │              │          │
│              │              │              │              │              │          │
│              │ Assign vehicle & driver      │              │              │          │
│              │ ────────────►│              │              │              │          │
│              │              │              │              │              │          │
│              │              │ Assign driver│              │              │          │
│              │◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
│              │ Status: CHỜ_GIAO            │              │              │          │
│              │              │              │              │              │          │
│              │              │              │ Receive delivery          │          │
│              │              │              │ ◄─────────────│          │          │
│              │              │              │              │              │          │
│              │              │              │ Start delivery│          │          │
│              │              │              │ ─────────────►│          │          │
│              │              │              │              │              │          │
│              │ Update: ĐANG_GIAO            │              │              │          │
│              │◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
│              │              │              │              │ GPS tracking │          │
│              │              │              │              │ ◄─────────────│          │
│              │              │              │              │              │          │
│              │              │              │              │ Attempt delivery        │
│              │              │              │              │ ────────────►│          │
│              │              │              │              │              │          │
│         ┌────┴────┐         │              │              │              │          │
│         │ SUCCESS │         │              │              │              │          │
│         └────┬────┘         │              │              │              │          │
│              │              │              │              │              │          │
│              │              │              │ Collect POD (signature + photo)│     │
│              │              │              │ ◄─────────────│              │          │
│              │              │              │              │              │          │
│              │              │              │ Confirm delivery complete     │
│              │◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
│              │ Status: HOAN_THANH│         │              │              │          │
│              │◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
│              │              │              │              │ Send notification        │
│              │◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
│              │ Sync: Đơn hoàn thành         │              │              │          │
│              │              │              │              │ ────────────►│          │
│              │              │              │              │              │          │
│         ┌────┴────┐         │              │              │              │          │
│         │  FAIL   │         │              │              │              │          │
│         └────┬────┘         │              │              │              │          │
│              │              │              │              │              │          │
│              │              │              │ Record failure reason        │
│              │              │              │ ◄─────────────│              │          │
│              │              │              │              │              │          │
│              │ Status: GIAO_THAT_BAI       │              │              │          │
│              │              │              │              │              │          │
│              │ Return goods to warehouse   │              │              │          │
│              │ ◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
│              │              │              │              │ Notify Sale │          │
│              │◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
└──────────────┴──────────────┴──────────────┴──────────────┴──────────────┴──────────┘

Status Flow:
CHỜ_GIAO → ĐANG_GIAO → HOAN_THANH / GIAO_THAT_BAI
```

#### StockTake Process (Kiểm kê)
```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ STOCKTAKE PROCESS SWIMLANES                                                        │
├──────────────┬──────────────┬──────────────┬──────────────┬──────────────┬──────────┤
│  WH MANAGER  │    SYSTEM    │   STAFF      │   ACCOUNTING │    QC TEAM   │  AUDIT  │
├──────────────┼──────────────┼──────────────┼──────────────┼──────────────┼──────────┤
│              │              │              │              │              │          │
│ Create stocktake          │              │              │              │          │
│ ────────────►│              │              │              │              │          │
│              │              │              │              │              │          │
│              │ Generate product list      │              │              │          │
│              │ ◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
│              │              │ Count physical              │              │          │
│              │◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
│              │ Compare: System vs Physical │              │              │          │
│              │ ◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
│              │ Generate variance report    │              │              │          │
│              │ ────────────►│              │              │              │          │
│              │              │              │              │              │          │
│              │              │ Review & confirm variance   │              │          │
│              │◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
│         ┌────┴────┐         │              │              │              │          │
│         │  NORMAL │         │              │              │              │          │
│         │ VARIANCE│         │              │              │              │          │
│         └────┬────┘         │              │              │              │          │
│              │              │              │              │              │          │
│              │              │              │ Review variance report     │
│              │              │              │ ◄─────────────│              │          │
│              │              │              │              │              │          │
│ Approve adjustment       │              │              │              │          │
│ ────────────►│              │              │              │              │          │
│              │              │              │              │              │          │
│              │ Update inventory           │              │              │          │
│              │ ────────────►│              │              │              │          │
│              │              │              │              │              │          │
│              │              │              │              │              │ Log adjustment
│              │              │              │              │ ─────────────►│
│              │              │              │              │              │          │
│              │              │              │ Sync to Accounting         │
│              │              │              │ ◄─────────────│              │
│              │              │              │              │              │          │
│              │              │              │              │              │          │
│         ┌────┴────┐         │              │              │              │          │
│         │ DAMAGE/ │         │              │              │              │          │
│         │ LOSS    │         │              │              │              │          │
│         └────┬────┘         │              │              │              │          │
│              │              │              │              │              │          │
│              │              │              │              │ Create damage report      │
│              │              │              │              │ ◄─────────────│          │
│              │              │              │              │              │          │
│              │              │ Move to QUARANTINE          │              │          │
│              │◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
│              │              │              │              │ Notify WH Manager        │
│              │◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
└──────────────┴──────────────┴──────────────┴──────────────┴──────────────┴──────────┘

Status Flow:
PENDING → IN_PROGRESS → PENDING_APPROVAL → APPROVED → COMPLETED
```

#### Return Process (Hoàn hàng từ Đại lý)
```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ RETURN PROCESS SWIMLANES                                                            │
├──────────────┬──────────────┬──────────────┬──────────────┬──────────────┬──────────┤
│    DEALER    │    SALE      │    SYSTEM    │   ACCOUNTING │      QC      │  STORE  │
├──────────────┼──────────────┼──────────────┼──────────────┼──────────────┼──────────┤
│              │              │              │              │              │          │
│ Request return              │              │              │              │          │
│ ────────────►│              │              │              │              │          │
│              │              │              │              │              │          │
│              │ Confirm return request     │              │              │          │
│              │ ────────────►│              │              │              │          │
│              │              │              │              │              │          │
│              │              │ Create Return Receipt     │              │          │
│              │              │ ─────────────►│              │              │          │
│              │              │              │              │              │          │
│              │              │              │              │ Receive goods            │
│              │              │              │              │ ◄─────────────│          │
│              │              │              │              │              │          │
│              │              │              │              │ QC inspection            │
│              │              │              │              │ ─────────────►│          │
│              │              │              │              │              │          │
│              │              │         ┌────┴────┐         │              │          │
│              │              │         │  PASS   │         │              │          │
│              │              │         └────┬────┘         │              │          │
│              │              │              │              │              │          │
│              │              │              │              │ Update stock (normal)    │
│              │              │              │              │ ◄─────────────│          │
│              │              │              │              │              │          │
│              │              │ Send credit note request   │              │          │
│              │              │ ─────────────►│              │              │          │
│              │              │              │              │              │          │
│              │              │              │ Create credit note│          │          │
│              │              │              │ ◄─────────────│              │          │
│              │              │              │              │              │          │
│              │ Notify dealer (credit processed)         │              │          │
│              │ ◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
│              │              │         ┌────┴────┐         │              │          │
│              │              │         │  FAIL   │         │              │          │
│              │              │         └────┬────┘         │              │          │
│              │              │              │              │              │          │
│              │              │              │              │ Move to QUARANTINE        │
│              │              │              │              │ ◄─────────────│          │
│              │              │              │              │              │          │
│              │              │              │              │ Create damage report     │
│              │              │              │              │ ─────────────►│          │
│              │              │              │              │              │          │
│              │              │              │              │ Notify WH Manager        │
│              │              │◄─────────────│              │              │          │
│              │              │              │              │              │          │
│              │              │              │              │              │ Mark for disposal
│              │              │              │              │ ◄─────────────│          │
│              │              │              │              │              │          │
└──────────────┴──────────────┴──────────────┴──────────────┴──────────────┴──────────┘

Status Flow:
PENDING → RECEIVED → QC_PASS / QC_FAIL → PROCESSED
```

#### Inventory Adjustment Process (Điều chỉnh tồn kho)
```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ INVENTORY ADJUSTMENT PROCESS SWIMLANES                                              │
├──────────────┬──────────────┬──────────────┬──────────────┬──────────────────────────┤
│   STOREKEEPER│    SYSTEM    │  WH MANAGER │   ACCOUNTING │        AUDIT            │
├──────────────┼──────────────┼──────────────┼──────────────┼──────────────────────────┤
│              │              │              │              │                         │
│ Detect discrepancy          │              │              │                         │
│ ────────────►│              │              │              │                         │
│              │              │              │              │                         │
│              │ Create adjustment request │              │                         │
│              │ ────────────►│              │              │                         │
│              │              │              │              │                         │
│              │              │ Review request│              │                         │
│              │              │ ◄─────────────│              │                         │
│              │              │              │              │                         │
│         ┌────┴────┐         │              │              │                         │
│         │ MINOR   │         │              │              │                         │
│         │ (< 5%) │         │              │              │                         │
│         └────┬────┘         │              │              │                         │
│              │              │              │              │                         │
│              │              │ Approve & update stock     │
│              │              │ ◄─────────────│              │                         │
│              │              │              │              │                         │
│              │ Update inventory           │              │                         │
│              │ ◄─────────────│              │              │                         │
│              │              │              │              │                         │
│              │              │              │              │ Log adjustment           │
│              │              │              │              │ ◄─────────────│          │
│              │              │              │              │                         │
│              │              │              │ Sync to Accounting│                     │
│              │              │              │ ◄─────────────│          │          │
│              │              │              │              │                         │
│              │              │              │              │                         │
│         ┌────┴────┐         │              │              │                         │
│         │ MAJOR   │         │              │              │                         │
│         │ (> 5%) │         │              │              │                         │
│         └────┬────┘         │              │              │                         │
│              │              │              │              │                         │
│              │              │ Require investigation       │
│              │              │ ─────────────►│              │                         │
│              │              │              │              │                         │
│              │              │ Submit investigation report│              │                         │
│              │◄─────────────│              │              │                         │
│              │              │              │              │                         │
│              │              │ Final approval│              │                         │
│              │              │ ◄─────────────│              │                         │
│              │              │              │              │                         │
│              │ Update inventory           │              │                         │
│              │ ◄─────────────│              │              │                         │
│              │              │              │              │                         │
│              │              │              │              │ Log with investigation  │
│              │              │              │              │ ◄─────────────│          │
│              │              │              │              │                         │
└──────────────┴──────────────┴──────────────┴──────────────┴──────────────────────────┘

Status Flow:
PENDING → REVIEWING → APPROVED → COMPLETED
```

---

## ANTI-PATTERNS (Tránh xa)

### Code Anti-Patterns

| Anti-Pattern | Description | How to Avoid |
|--------------|-------------|--------------|
| **God Class/Service** | Một class/service quá lớn, làm mọi thứ | Max 300 lines/file, 40 lines/function |
| **Anemic Domain Model** | Entities chỉ có getters/setters, không có behavior | Move logic vào domain methods |
| **Circular Dependencies** | A → B → C → A | Use interfaces, dependency injection |
| **Premature Optimization** | Tối ưu sớm, phức tạp hóa code không cần thiết | YAGNI - You Ain't Gonna Need It |
| **Magic Numbers** | Hard-coded numbers không có constant | Use named constants (e.g., `MAX_RETRY = 3`) |
| **Dead Code** | Code không bao giờ được gọi | Remove unused code, keep clean |
| **Shotgun Surgery** | Một thay đổi cần sửa nhiều file không liên quan | Keep related code together |

### Database Anti-Patterns

| Anti-Pattern | Description | How to Avoid |
|--------------|-------------|--------------|
| **N+1 Query Problem** | 1 query, rồi N queries cho mỗi item | Use JOIN FETCH, @EntityGraph |
| **Missing Indexes** | Chậm khi query lớn | Analyze queries, add indexes |
| **Denormalization Abuse** | Quá nhiều denormalized tables | Balance read vs write performance |
| **Soft Deletes Everywhere** | Xóa mềm mọi thứ | Only soft delete audit-critical data |
| **EAV (Entity-Attribute-Value)** | Dynamic attributes = performance nightmare | Use proper columns, JSON for true dynamic |

### Spring Boot Anti-Patterns

| Anti-Pattern | Description | How to Avoid |
|--------------|-------------|--------------|
| **Anemic Services** | Service chỉ gọi repository, không có logic | Add business logic to service layer |
| **God Controllers** | Controller xử lý quá nhiều, validate, transform | Use DTOs, validation annotations |
| **Transaction Failures** | @Transactional không used đúng | Understand propagation, isolation levels |
| **N+1 in JPA** | Lazy loading gây N+1 | Use @Fetch(FetchMode.JOIN) or JOIN FETCH |
| **Oversized @Entity** | Entity có 50+ columns | Split into smaller entities/Value Objects |

### React Anti-Patterns

| Anti-Pattern | Description | How to Avoid |
|--------------|-------------|--------------|
| **Prop Drilling** | Pass props qua nhiều levels không cần | Use Context or Zustand store |
| **Memory Leaks** | Unmounted component still listens | Cleanup in useEffect return |
| **Inline Functions in JSX** | `<button onClick={() => handleClick()}>` | Define outside render or use useCallback |
| **Overfetching** | Fetch entire data when only need summary | Use GraphQL fields or API filters |
| **God Components** | One component 1000+ lines | Split into smaller, focused components |

### Integration Anti-Patterns

| Anti-Pattern | Description | How to Avoid |
|--------------|-------------|--------------|
| **Chatty APIs** | Nhiều API calls nhỏ thay vì batch | Batch operations, composite APIs |
| **Synchronous Everything** | Blocking calls for async operations | Use events, queues for long-running ops |
| **Ignoring Failures** | Catch exception, do nothing | Always handle exceptions properly |
| **Hard-coded Timeouts** | Magic numbers for timeouts | Use configuration with sensible defaults |

---

## TESTING ANTI-PATTERNS TO AVOID

| Anti-Pattern | Description | Fix |
|--------------|-------------|-----|
| **Test for coverage** | Viết test chỉ để pass coverage metric | Write meaningful tests |
| **Brittle selectors** | Test break when refactor UI | Use data-testid attributes |
| **No assertion** | Tests that only execute code | Always assert expected outcomes |
| **Shared mutable state** | Tests affect each other | Each test = independent |
| **Slow tests** | Unit tests hitting real DB | Mock dependencies, use testcontainers |

---

