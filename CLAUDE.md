# CLAUDE.md — Project Memory
# Warehouse Management System (WMS)
# Đọc file AGENTS.md trước để hiểu full project context

## MANUAL MEMORY (human-maintained)

### Architecture Decisions (ADR)
- **ADR-001**: Chọn Spring Boot 3.4.5 + Java 21 thay vì Node.js vì team có kinh nghiệm Java enterprise
- **ADR-002**: PostgreSQL 18 với Spring Data JPA/Hibernate cho ORM - type-safety và relational integrity
- **ADR-003**: React 18 + TypeScript cho frontend - component-based, strong typing
- **ADR-004**: Tailwind CSS 3.x cho styling - utility-first, maintainable
- **ADR-005**: JWT + bcrypt cho auth - stateless, secure với cost factor 12
- **ADR-006**: Tách Kho ảo "In-Transit Location" để tracking hàng đang di chuyển giữa các kho
- **ADR-007**: Quarantine Zone cho hàng lỗi/không đạt QC - không tính vào tồn kho khả dụng

### Lessons Learned (từ incidents và code review)
- **LESSON-001**: Luôn validate tồn kho TRƯỚC khi xuất - không cho âm kho dưới mọi hoàn cảnh
- **LESSON-002**: Quản lý theo Lô (Batch) với Grade duy nhất - tránh conflict khi xuất FEFO/FIFO
- **LESSON-003**: Tất cả nghiệp vụ kho phải gửi event sang Kế toán qua message queue
- **LESSON-004**: Phân quyền phải kiểm tra BOTH vai trò (role) VÀ kho được gán (warehouse assignment)
- **LESSON-005**: Barcode scanning phải có fallback nhập tay khi scan fail

### Current Sprint Notes
- **Sprint 1**: Core Warehouse Operations (FR-WH-01 đến FR-WH-04)
- **Blocked**: Integration specs với Kế toán/HRM/Sale API còn pending
- **Next**: Implement batch management với FEFO logic

## PATTERNS TO FOLLOW
- **Service pattern**: `src/main/java/com/wms/service/[Name]Service.java`
- **Controller pattern**: `src/main/java/com/wms/controller/[Name]Controller.java`
- **Repository pattern**: `src/main/java/com/wms/repository/[Name]Repository.java`
- **Entity pattern**: `src/main/java/com/wms/entity/[Name].java`
- **DTO pattern**: `src/main/java/com/wms/dto/[Name]DTO.java`
- **Error codes**: use constants from `src/main/java/com/wms/constants/ErrorCodes.java`
- **Always create**: unit test alongside service class
- **Always log**: audit trail cho mọi nghiệp vụ kho (who, when, what)

## TECH STACK
> ⚠️ **Single Source of Truth**: Xem `AGENTS.md` Section 2 để biết đầy đủ Tech Stack.
> Không duy trì bản sao ở đây để tránh mâu thuẫn khi có thay đổi.

## NAMING CONVENTIONS
- Java classes: PascalCase (e.g., WarehouseService.java)
- Java packages: lowercase (e.g., com.wms.service)
- Database tables: snake_case (e.g., warehouse_staff)
- API endpoints: kebab-case (e.g., /api/warehouse-stock)
- React components: PascalCase (e.g., WarehouseCard.tsx)
- React hooks/utilities: camelCase (e.g., useWarehouse.ts)

## KARPATHY PRINCIPLES 
1. **Plan first** - always use planning mode for non-trivial tasks
2. **Verify constantly** - observe like a hawk in IDE, check assumptions
3. **Keep it simple** - prefer solutions under 100 lines, avoid over-engineering
4. **Only edit necessary** - don't change unrelated code, no "improvements" on working code
5. **Focus on results** - define success criteria, write tests first
6. **Use sub-agents for research** - keep context clean, each agent one task

## AUTO MEMORY (Claude Code appends here)
<!-- Claude Code sẽ tự động thêm entries khi bạn làm việc -->
