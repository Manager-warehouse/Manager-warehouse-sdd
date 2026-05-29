# Research: 3-Week Sprint - Core WMS Operations

## Resolved Clarifications

### Decision: Spring Boot + Java 21 (Existing ADR-001)
**Rationale**: Team co kinh nghiem Java enterprise, type safety manh, ecosystem on dinh. Duoc xac nhan trong CLAUDE.md va AGENTS.md.
**Alternatives considered**: Node.js (rejected - team khong co kinh nghiem), Python/Django (rejected - performance va type safety khong bang Java).

### Decision: PostgreSQL 18 + Flyway (Existing ADR-002)
**Rationale**: ORM type safety, relational integrity, Flyway cho migration versioning. Duoc xac nhan trong tech stack.
**Alternatives considered**: MySQL (rejected - PostgreSQL co hro hon cho JSONB, CTE, UPSERT).

### Decision: JWT + bcrypt cost 12 (Existing ADR-003)
**Rationale**: Stateless authentication, scalable, industry standard. Cost 12 dam bao brute-force resistance.
**Alternatives considered**: Session-based auth (rejected - can stateful server), OAuth2 (rejected - overkill cho internal system).

### Decision: In-Transit Virtual Warehouse (Existing ADR-004)
**Rationale**: Track inventory trong luc van chuyen giua cac kho, khong mat visibility. Duoc xac nhan trong CLAUDE.md.
**Alternatives considered**: Direct transfer without tracking (rejected - khong co audit trail), Message queue event (rejected - ADR-006 chon dong bo truc tiep).

### Decision: Quarantine Zone (Existing ADR-005)
**Rationale**: Tach hang loi khoi available inventory, track rejects separately.
**Alternatives considered**: Soft-delete failed items (rejected - mat thong tin reject), Same zone with flag (rejected - phuc tap inventory calculation).

### Decision: Accounting chung DB (Existing ADR-006)
**Rationale**: Don gian hoa kien truc, dam bao ACID transactions, khong can Message Queue.
**Alternatives considered**: Separate accounting service + MQ (rejected - overkill cho scope hien tai).

### Decision: FEFO/FIFO Implementation
**Rationale**: FEFO cho san pham co expiry date, FIFO cho san pham khong co expiry. Implement trong BatchService.
**Alternatives considered**: Only FIFO (rejected - vi pham domain rule cho hang co expiry), Only FEFO (rejected - khong phu hop cho hang khong co expiry).

### Decision: Optimistic Locking (@Version)
**Rationale**: Tranh concurrent write conflicts tren inventory updates. Su dung JPA @Version annotation.
**Alternatives considered**: Pessimistic locking (rejected - performance hit, deadlock risk), Application-level lock (rejected - khong dam bao consistency).

### Decision: Audit Log via Spring AOP
**Rationale**: Aspect-oriented approach cho phep capture tat ca service method calls without boilerplate. AspectJ pointcut tren @Service layer.
**Alternatives considered**: Manual logging trong moi method (rejected - boilerplate code, de quen), DB trigger (rejected - kho maintain, hard to test).

### Decision: Frontend State Management - Zustand
**Rationale**: Don gian hon Redux, TypeScript-friendly, built-in middleware cho devtools/persistence.
**Alternatives considered**: Redux Toolkit (rejected - nhieu boilerplate cho project size nay), React Context (rejected - performance issues voi frequent updates), React Query (co the ket hop sau cho server state).

### Decision: MapStruct for DTO mapping
**Rationale**: Generate type-safe mapper code tai compile time, giam boilerplate so voi manual mapping.
**Alternatives considered**: Manual mapping (rejected - nhieu boilerplate), ModelMapper (rejected - runtime reflection, cham hon, loi kho debug).

### Decision: React Router 6 + React Hook Form
**Rationale**: React Router 6 la chuan cho SPA routing. React Hook Form giam re-renders va validation boilerplate.
**Alternatives considered**: Formik (rejected - performance kem hon React Hook Form), Remix (rejected - can server runtime).

## Technology Best Practices

### Spring Boot Best Practices
- Layered architecture: Controller -> Service -> Repository -> Entity
- Constructor injection (final fields, @RequiredArgsConstructor)
- @Validated + Jakarta Validation annotations on DTOs
- @RestControllerAdvice cho global exception handling
- @Transactional(readOnly = true) cho GET, @Transactional cho write operations
- DTOs tach biet cho request va response (khong dung entity truc tiep)
- Use @Query with JOIN FETCH de tranh N+1
- Pagination: Spring Data Pageable cho list endpoints

### JPA/Hibernate Best Practices
- @EntityGraph(name = "Inventory.withBatch", attributePaths = {"batch", "product"})
- @Fetch(FetchMode.JOIN) cho critical relationships
- @Version cho optimistic locking
- Flyway migrations: V1__init.sql, V2__add_column.sql, etc.
- Indexes: composite indexes cho frequent query patterns (warehouse_id + product_id + batch_id)
- Avoid EAGER fetching - use explicit JOIN FETCH or @EntityGraph

### React 18 + Tailwind Best Practices
- Functional components + hooks (no class components)
- Custom hooks cho reusable logic (useApi, useAuth, usePagination)
- Zustand stores: tach biet cho tung domain (authStore, inventoryStore, receiptStore)
- Axios interceptors cho JWT token refresh + error handling
- React.memo cho expensive components
- Tailwind: use @apply cho repeated patterns

### Security Best Practices
- JWT: access token (15 phut) + refresh token (7 ngay) pattern
- bcrypt cost 12 cho password hashing
- Password: min 8 chars, hoa/thuong/so/dac biet
- CORS restrict to known origins
- Rate limiting cho login attempts (5 failures -> 15 min lockout)
- Principal injection: @AuthenticationPrincipal UserPrincipal
- Method-level security: @PreAuthorize("hasRole('ADMIN')")

## Dependency Decisions

### Backend Dependencies (pom.xml)
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-security
- spring-boot-starter-validation
- flyway-core + flyway-database-postgresql
- lombok
- mapstruct + mapstruct-processor
- springdoc-openapi-starter-webmvc-ui (Swagger)
- jjwt-api + jjwt-impl + jjwt-jackson (JWT)
- postgresql (runtime)
- h2 (test)

### Frontend Dependencies (package.json)
- react 18, react-dom 18
- react-router-dom 6
- axios
- zustand
- react-hook-form + @hookform/resolvers
- lucide-react (icons)
- tailwindcss 3.x + postcss + autoprefixer
- @tailwindcss/forms
- dayjs (date formatting)
- react-hot-toast (notifications)
- @tanstack/react-table v8 (data tables)
- jest + @testing-library/react (test)

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Concurrent inventory write conflict | Medium | High | @Version optimistic locking, retry mechanism |
| N+1 query performance | Medium | Medium | JOIN FETCH, @EntityGraph review in code review |
| JWT token security | Low | High | Short-lived access tokens, rotation, secure HTTP-only cookies |
| Frontend deadline slippage | Medium | Medium | Mock API cho FE dev, parallel work |
| Database migration conflicts | Low | High | Flyway checksum validation, unique migration IDs |
| FEFO/FIFO calculation bugs | Medium | High | Extensive unit tests, edge case coverage |
