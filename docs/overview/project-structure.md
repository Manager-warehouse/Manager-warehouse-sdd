# Sơ đồ dự án — Manager Warehouse SDD

> Đồng bộ: 2026-07-15. Đây là sơ đồ logical của các thư mục đang dùng; không liệt kê thư mục build/cache như `target`, `dist`, `node_modules`.

```text
Manager-warehouse-sdd/
├── .sdd/
│   ├── constitution.md, shared_context.md, constraints/
│   ├── docs/                       # RDS, SDS, database schema documentation
│   └── specs/001-... đến 012-...   # Nguồn đặc tả feature
├── .specify/
│   ├── memory/constitution.md      # Constitution canonical
│   ├── templates/                  # Spec/plan/task templates
│   └── scripts/                    # Utility scripts của Specify
├── .agents/                        # Agent configuration và Speckit skills
├── .claude/                        # Claude/GitNexus local skill configuration nếu có
├── backend/
│   ├── src/main/java/com/wms/      # Controller → Service → Repository → Entity
│   ├── src/main/resources/db/migration/ # Flyway migrations
│   └── src/test/java/              # JUnit/Mockito/integration tests
├── frontend/
│   ├── src/                        # React pages, components, routes, services, stores
│   ├── tests/                      # Vitest + React Testing Library
│   ├── android/ và ios/            # Capacitor native shells
│   └── package.json
├── selenium-tests/                 # UI/end-to-end test project nếu được sử dụng
├── specs/                          # Repo-level delivery/production specs
├── scripts/                        # Deploy and maintenance scripts
├── AGENTS.md, CLAUDE.md            # Quy tắc vận hành và kiến trúc cho agent
├── README.md                       # Giới thiệu dự án và bản đồ tài liệu
└── docs/
    ├── overview/
    │   ├── actors.md
    │   ├── user-stories.md
    │   ├── features-summary.md
    │   └── project-structure.md
    ├── architecture/
    │   ├── design-system.md
    │   └── frontend-structure.md
    ├── deployment/
    │   ├── deployment-guide.md
    │   ├── docker-compose-guide.md
    │   └── mobile-build-guide.md
    └── internal/
        └── diagram-skill-entry.md
```

## Nguồn sự thật và phạm vi

| Nội dung | Nguồn chuẩn |
| --- | --- |
| Quy tắc kiến trúc, data integrity, test/DoD | `.specify/memory/constitution.md` |
| Quy tắc vận hành agent và domain | `AGENTS.md`, `CLAUDE.md` |
| Requirement/acceptance criteria | `.sdd/specs/001`–`012` |
| Thiết kế yêu cầu và lớp inbound đại diện | `.sdd/docs/RDS-WMS.md`, `.sdd/docs/SDS-WMS.md` |
| Tài liệu schema | `.sdd/docs/database_schema_documentation.md` + Flyway migrations |

Spec 001–010 bao phủ nghiệp vụ WMS. Spec 011 (backend testing/SonarQube) và 012 (frontend testing) là chất lượng xuyên suốt, không bổ sung actor nghiệp vụ hoặc bảng dữ liệu business.
