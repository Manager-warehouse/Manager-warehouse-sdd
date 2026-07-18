# Diagram Toolkit — gói dùng chung

Điểm bắt đầu duy nhất: [`.agents/skills/diagram-toolkit/SKILL.md`](../.agents/skills/diagram-toolkit/SKILL.md).

## Dùng trong 30 giây

1. Nêu mục tiêu: “vẽ luồng duyệt đơn”, “vẽ ERD database”, “vẽ class diagram cho module …”.
2. Skill chọn loại sơ đồ và engine; mặc định là Mermaid để nhúng thẳng vào Markdown.
3. Cung cấp nguồn sự thật: requirement, code, hoặc metadata database. Skill không được tự bịa quan hệ/thuộc tính.
4. Kiểm tra lại diagram theo checklist trước khi hoàn tất.

## Chọn nhanh

| Muốn làm rõ | Sơ đồ |
|---|---|
| Phạm vi hệ thống / actor | Context hoặc use-case |
| Quy trình nhiều vai | Swimlane / activity |
| Trạng thái của entity | State |
| Cấu trúc database | ERD + table dictionary |
| Quan hệ code | UML class |
| Thứ tự gọi API/service | Sequence |
| Kiến trúc thành phần | Component / architecture |

## Cấu trúc gói chính

```text
.agents/skills/diagram-toolkit/
├── SKILL.md                    # hướng dẫn duy nhất cần đọc đầu tiên
├── references/
│   ├── diagram-rules.md        # notation và checklist chất lượng
│   └── engine-choice.md        # khi nào dùng Mermaid/PlantUML/D2/BPMN/DBML
├── assets/                     # 4 template Mermaid cơ bản
└── scripts/mermaid-verify.mjs  # compile-check Mermaid
```

## Nội dung cũ

Các thư mục `claude-code/`, `explain-skills/`, `huong-dan/` và `example/` được giữ làm **tài liệu/engine tham khảo nâng cao**. Không cần đọc chúng để bắt đầu; dùng chúng chỉ khi cần PlantUML, D2, BPMN hoặc DBML chuyên biệt.
