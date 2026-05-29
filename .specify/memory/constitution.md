# WMS Phuc Anh Constitution

## Core Principles

### I. Layered Architecture (NON-NEGOTIABLE)
Backend phan tang bat buoc: Controller (@RestController) -> Service (@Service) -> Repository (@Repository) -> Entity (@Entity). Controller chi xu ly HTTP request/response. Service chua business logic. Repository chi lam viec voi JPA. Entity map truc tiep voi DB table.

### II. Inventory Integrity (NON-NEGOTIABLE)
`inventory.quantity >= 0` luon dung truoc va sau moi thao tac. Khong cho phep negative inventory. Moi thao tac UPDATE inventory phai co @Version optimistic locking. Moi thao tac phai tao audit log (actor, action, timestamp, before/after).

### III. FEFO/FIFO Batch Selection
San pham co expiry: chon batch co han dung gan nhat (FEFO). San pham khong co expiry: chon batch co received_date cu nhat (FIFO). Batch het han khong duoc chon cho xuat kho thong thuong.

### IV. QC Gate & Quarantine
Hang fail QC bat buoc vao Quarantine Zone. Quarantine inventory KHONG duoc tinh vao available inventory. Hang trong Quarantine can quyet dinh xu ly (tieu huy/tra NCC) truoc khi xoa khoi quarantine.

### V. In-Transit Tracking
Dieu chuyen kho bat buoc di qua In-Transit location. Chi khi kho dich xac nhan nhan hang thi inventory moi duoc cap nhat. Chenh lech quantity_sent vs quantity_received phai tao adjustment record.

### VI. Auth & RBAC
JWT + bcrypt (cost factor >= 12). Phan quyen phai check BOTH role AND warehouse (RBAC theo chi nhanh). Nhan vien kho Hai Phong khong duoc xem du lieu kho Ha Noi.

### VII. Test Coverage (NON-NEGOTIABLE)
Service logic moi: coverage toi thieu 80%. Unit test bat buoc cho FEFO/FIFO selection, credit check, inventory validation. Integration tests cho moi API endpoint.

## Additional Constraints

### Tech Stack (STRICT - do not deviate)
- Backend: Spring Boot 3.4.5 + Java 21 (Maven)
- Frontend: React 18 + JavaScript
- Database: PostgreSQL 18
- ORM: Spring Data JPA / Hibernate (KHONG raw SQL)
- Auth: JWT + bcrypt (cost >= 12)
- Styling: Tailwind CSS 3.x
- DB Migration: Flyway
- API Docs: OpenAPI / Swagger

### Code Quality
- Max function length: 40 lines
- Max file length: 300 lines
- Comments giai thich WHY, khong giai thich WHAT
- KHONG co System.out / console.log trong production code
- KHONG co TODO comments trong completed code
- Constructor injection (preferred)

### Domain Rules
- Master data: soft delete (is_active = false)
- Transaction data: status = cancelled (khong xoa vinh vien)
- Moi transaction chi co 1 grade (A/B/C). Khac grade = tao batch moi
- San pham has_serial = true bat buoc nhap/xuat serial

## Development Workflow

### Branching
- feat/*, fix/*, spec/*, chore/*
- KHONG commit truc tiep vao main/production
- Moi feature la mot nhanh nho, merge bang PR

### Definition of Done
1. Unit tests written and passing (>= 80% coverage)
2. Integration tests cho API endpoints (happy + error paths)
3. No linting/type errors
4. API endpoint documented in OpenAPI/Swagger
5. Error cases handled voi proper HTTP status codes
6. Audit log entry created cho warehouse operations
7. No TODO comments in code
8. FEFO/FIFO logic tested

## Governance

Constitution nay ap dung cho toan bo du an WMS Phuc Anh. Moi thay doi can duoc approve boi technical lead va update vao file nay.

**Version**: 1.0 | **Ratified**: 2026-05-29 | **Last Amended**: 2026-05-29
