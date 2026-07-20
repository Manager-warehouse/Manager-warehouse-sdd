# Global Constraints — WMS Phúc Anh

> Ràng buộc Tech stack, naming conventions, tooling bắt buộc.
> Nguồn: AGENTS.md · README.md · constitution.md

## 1. Tech Stack (Bất Di Bất Dịch)

| Layer | Công nghệ | Ghi chú |
|---|---|---|
| Backend | Spring Boot 3.4.5 + Java 21 (Maven) | JDK temurin, Java 21 LTS |
| Frontend | React 18 + JavaScript | KHÔNG TypeScript |
| Database | PostgreSQL 18 | UTF-8, Timezone UTC+7 |
| ORM | Spring Data JPA / Hibernate | KHÔNG raw SQL |
| Auth | JWT + bcrypt (cost ≥ 12) | `Authorization: Bearer {token}` |
| Styling | Tailwind CSS 3.x | KHÔNG CSS modules riêng lẻ |
| API Docs | OpenAPI / Swagger | Cập nhật khi thêm/sửa endpoint |
| DB Migration | Flyway | Migration files không xóa sau khi áp dụng |
| Testing BE | JUnit 5 + Mockito | Coverage ≥ 80% services |
| Testing FE | Jest + React Testing Library | Component test có business logic |
| Build BE | Maven | `mvn clean compile` |
| Build FE | Vite | `npm run build` |

## 2. File & Naming Conventions

| Loại | Convention | Ví dụ |
|---|---|---|
| Java class | PascalCase | `InventoryService.java` |
| Java package | lowercase | `com.wms.service` |
| React component | PascalCase | `ReceiptTable.jsx` |
| React hook/util | camelCase | `formatQuantity.js`, `useTransferFilters.js` |
| API endpoint | kebab-case | `/api/v1/warehouse-stock` |
| Database table | snake_case | `inventory_transactions` |
| DB column | snake_case | `product_id`, `bin_capacity` |
| DB constant/enum | UPPER_SNAKE | `ACTIVE`, `CREDIT_HOLD` |
| Java constant | UPPER_SNAKE | `MAX_BATCH_QUANTITY` |
| Java method | camelCase | `findByWarehouseId()` |

## 3. Backend Structure

```
backend/src/main/java/com/wms/
├── aop/          # AuditLoggingAspect, ExceptionHandlingAspect, PerformanceAspect
├── config/       # SecurityConfig, JpaConfig, SwaggerConfig, ApplicationProperties
├── controller/   # REST controllers — KHÔNG business logic
├── dto/          # request/response DTOs + Jakarta Validation annotations
├── entity/       # JPA entities (@Entity, @Table, relationships)
├── enums/        # Domain enums (WarehouseStatus, ReceiptStatus, ...)
├── event/        # Domain events, audit events
├── exception/    # Custom exceptions + GlobalExceptionHandler (@ControllerAdvice)
├── repository/   # Spring Data JPA interfaces
├── service/      # Business logic, @Transactional, audit logging
└── util/         # Helper utilities (FIFOSelector, mappers)
```

## 4. Frontend Structure

```
frontend/src/
├── components/common/    # Shared UI: Button, Table, Modal, Pagination, DatePicker...
├── components/warehouse/ # Nghiệp vụ kho: ReceiptPanel, IssueForm, TransferList...
├── hooks/                # Custom hooks: useFetch, useFilters, usePagination...
├── pages/                # Page-level components / routes
├── services/             # API client (Axios + JWT interceptor)
├── stores/               # Zustand global state
├── types/                # Shared type definitions
└── utils/                # formatCurrency, formatDate, quantity helpers...
```

## 5. REST API Standards

| Quy tắc | Chi tiết |
|---|---|
| Base URL | `/api/v1/[resource]` |
| Resource naming | kebab-case: `warehouse-stock`, `batch-management` |
| HTTP methods | GET (read), POST (create), PUT (update), PATCH (partial), DELETE (deactivate) |
| Status codes | 200, 201, 204, 400, 401, 403, 404, 409, 422, 500 |
| Error format | `{timestamp, status, error, message, path, details}` |
| Jakarta Validation | `@NotBlank`, `@NotNull`, `@Positive`, `@Size`... trên tất cả POST/PUT |
| Auth | JWT trong header `Authorization: Bearer {token}` |
| Public endpoints | Chỉ `/auth/login`, `/auth/refresh`, `/auth/forgot-password`, `/auth/verify-otp` (các endpoint bắt buộc trước khi có JWT) |

## 6. Code Quality Thresholds

| Metric | Threshold |
|---|---|
| Max function length | 40 lines |
| Max file length | 300 lines |
| Min service coverage | 80% lines + branches |
| Max PR size | 400 lines changed |
| Max method params | 5 params (vượt → dùng DTO/Builder) |

## 7. Tooling bắt buộc

| Tool | Mục đích |
|---|---|
| Flyway | Database migration versioning |
| SLF4J / Logback | Logging — KHÔNG `System.out` |
| Lombok | `@Slf4j`, `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`...; avoid `@Data` on JPA entities with lazy relationships |
| Swagger / OpenAPI | API documentation |

## 8. Database Conventions

- Tất cả bảng có `id BIGSERIAL PK`
- Tất cả bảng master data có `is_active BOOLEAN DEFAULT true`
- Tất cả bảng có `created_at`, `updated_at` (TIMESTAMP WITH TIME ZONE)
- Soft-delete master data = `is_active = false`
- Cancel transaction = `status = CANCELLED`
- Đơn vị tiền = `DECIMAL(18,2)`
- Optimistic locking = cột `version INTEGER DEFAULT 0`
