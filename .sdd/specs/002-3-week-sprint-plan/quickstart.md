# Quickstart — WMS Phuc Anh Development Setup

## Prerequisites

- Java 21 (JDK)
- Maven 3.9+
- Node.js 18+ + npm
- PostgreSQL 18
- Docker + Docker Compose (optional but recommended)
- Git

## 1. Clone & Branch

```bash
git clone <repo-url> Manager-warehouse-sdd
cd Manager-warehouse-sdd
git checkout 002-3-week-sprint-plan
```

## 2. Database Setup

### Option A: Docker Compose (Recommended)

```bash
docker compose up -d postgres
```

### Option B: Local PostgreSQL

```sql
CREATE DATABASE wms_phucanh;
CREATE USER wms_user WITH PASSWORD "wms_pass";
GRANT ALL PRIVILEGES ON DATABASE wms_phucanh TO wms_user;
```

## 3. Backend Setup

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Flyway Migrations

Flyway auto runs on startup. Migrations at:
`backend/src/main/resources/db/migration/`

### Seed Data

V1__init_schema.sql includes roles, default admin, 3 warehouses.

Default admin:
- Username: `admin`
- Password: `Admin@123456`

## 4. Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on http://localhost:5173

## 5. API Documentation

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## 6. Run Tests

### Backend
```bash
cd backend
mvn test
```

### Frontend
```bash
cd frontend
npm run test
```

## 7. Docker Full Stack
```bash
docker compose up -d
```

## 8. Development Workflow

```bash
git checkout -b feat/inbound-receipt-api
# Implement -> Test -> Commit
git commit -m "feat(receipt): add receipt creation with QC flow"
git checkout 002-3-week-sprint-plan
git merge feat/inbound-receipt-api
```

## 9. Key Configurations

See application.yml for:
- Database connection
- JWT secret & expiration
- Audit settings
- Approval thresholds

