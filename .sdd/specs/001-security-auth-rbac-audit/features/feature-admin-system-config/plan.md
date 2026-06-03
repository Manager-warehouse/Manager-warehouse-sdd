# Implementation Plan: System Admin Cấu hình Tham số (US-WMS-01)

**Branch**: `feat/son-sysemconfig-001` | **Date**: 2026-06-03 | **Spec**: `feature-admin-system-config.md`

## Summary

Cho phép người dùng có role `ADMIN` cấu hình 6 tham số vận hành cốt lõi (DEFAULT_CREDIT_LIMIT, DEFAULT_PAYMENT_TERM_DAYS, CREDIT_HOLD_OVERDUE_DAYS, CREDIT_UNLOCK_BUFFER_PCT, MONTHLY_CLOSING_DAY, MIN_INVENTORY_WARNING_THRESHOLD). Lưu lịch sử vào Audit Log khi có sự thay đổi giá trị.

## Technical Context

**Language/Version**: Java 21 / Spring Boot 3.4.5
**Primary Dependencies**: Spring Web, Spring Data JPA, Hibernate, Lombok
**Storage**: PostgreSQL 18
**Testing**: JUnit 5, Mockito
**Target Platform**: Backend API
**Project Type**: Web Service (REST API)
**Constraints**: Audit logging is mandatory for every config change. Must ensure strict validation for each key.

## Constitution Check
*GATE: Passed*
- Layered architecture strictly followed.
- No direct repository access from controllers.
- Test coverage requirement (80%) applied.

## Project Structure
```text
backend/
├── src/main/java/com/wms/
│   ├── controller/SystemConfigController.java
│   ├── dto/request/SystemConfigUpdateRequest.java
│   ├── dto/response/SystemConfigResponse.java
│   ├── mapper/SystemConfigMapper.java
│   ├── service/SystemConfigService.java
│   └── service/impl/SystemConfigServiceImpl.java
└── src/test/java/com/wms/service/SystemConfigServiceTest.java
```
**Structure Decision**: Standard Spring Boot Layered Architecture for single backend project.

## Complexity Tracking
No justified violations.
