# Feature: Cảnh báo tự động Tồn kho dưới định mức (US-WMS-26)

## 1. Context and Goal

Để tránh tình trạng gián đoạn kinh doanh do hết hàng hoặc thiếu hàng phục vụ các đơn xuất DO, hệ thống cần tự động giám sát mức tồn kho khả dụng của từng sản phẩm tại 3 kho vật lý. Khi tồn kho khả dụng chạm hoặc giảm xuống dưới mức định mức tối thiểu, hệ thống sẽ tự động kích hoạt cảnh báo, gửi thông báo khẩn và đánh dấu trên Dashboard để Trưởng kho và bộ phận Planner kịp thời xử lý (tái nhập hàng hoặc lập lệnh điều chuyển kho). Cảnh báo sẽ tự động được đóng (resolve) khi hàng hóa được bổ sung đầy đủ.

---

## 2. Actor

- **Trưởng kho (`WAREHOUSE_MANAGER`)** — Checker/Viewer hoạt động: Nhận cảnh báo và xem danh sách cảnh báo của kho mình được phân quyền phụ trách.
- **Planner (`PLANNER`)** — Maker điều phối: Nhận cảnh báo và xem danh sách cảnh báo trên phạm vi toàn quốc để ra quyết định điều chuyển/nhập hàng.

---

## 3. Functional Requirements (EARS)

### 3.1 Ubiquitous

- The system SHALL calculate available inventory for alert evaluation as: `available_qty = total_qty - reserved_qty` from regular quality-passed stock in the warehouse.
- The system SHALL use the product's warehouse-specific `reorder_point` as the warning threshold.
- WHERE a product's warehouse-specific `reorder_point` is null, the system SHALL fallback to the system-wide configuration parameter `MIN_INVENTORY_WARNING_THRESHOLD`.
- The system SHALL restrict Trưởng kho (`WAREHOUSE_MANAGER`) to only query and receive alerts for warehouses assigned to their user profile.
- The system SHALL allow Planner (`PLANNER`) to query and receive alerts across all warehouses in the system.

### 3.2 Event-driven

**Kích hoạt Cảnh báo tồn kho thấp**
- WHEN any inventory transaction (order reservation, picking completion, adjustment, scrap) causes the `available_qty` of product P at warehouse W to fall below the defined threshold:
  - Check if there is an active alert (`is_resolved = false` and `alert_type = 'LOW_STOCK'`) for `(warehouse_id = W, product_id = P)`.
  - IF no active alert exists:
    - Create a new record in `stock_alerts` with `is_resolved = false`, `current_qty = available_qty`, and `reorder_point = threshold`.
    - Generate an in-app notification (High Priority) with message: `[ALERT] Sản phẩm [SKU] tại [Kho] đã giảm dưới định mức tối thiểu (Tồn khả dụng: [Qty] / Ngưỡng: [Threshold])`.
    - Send this notification to all Planners and to the Trưởng kho assigned to warehouse W.
  - IF an active alert already exists:
    - Update `current_qty = available_qty` and `updated_at = NOW()` on the existing record (do not duplicate the alert).

**Tự động Giải quyết Cảnh báo (Auto-resolve)**
- WHEN an inbound receipt, adjustment, or inter-warehouse transfer receipt causes the `available_qty` of product P at warehouse W to rise back to or exceed the threshold:
  - Find the active alert record (`is_resolved = false`) for `(warehouse_id = W, product_id = P)`.
  - IF found:
    - Update the record: `is_resolved = true`, `resolved_at = NOW()`, `current_qty = available_qty`.
    - Generate an in-app info notification: `[RESOLVED] Sản phẩm [SKU] tại [Kho] đã được bổ sung đầy đủ (Tồn khả dụng: [Qty] / Ngưỡng: [Threshold])`.

**Truy vấn Cảnh báo**
- WHEN a user requests `GET /api/v1/alerts/low-stock`:
  - Validate warehouse scope permissions (Trưởng kho chỉ được xem kho được phân quyền).
  - Apply optional filters: `warehouse_id`, `product_id`, `is_resolved` (default returns both active and resolved).
  - Return HTTP 200 with the matching alert records sorted by `created_at DESC`.

---

## 4. API Endpoints

| Method | Path | Mô tả |
|--------|------|-------|
| `GET` | `/api/v1/alerts/low-stock` | Tra cứu danh sách cảnh báo tồn kho |

### Response — `GET /api/v1/alerts/low-stock`

```json
{
  "alerts": [
    {
      "id": 105,
      "warehouse_id": 1,
      "warehouse_name": "Kho Hải Phòng",
      "product_id": 42,
      "product_sku": "POT-001",
      "product_name": "Nồi inox 3 đáy Supor",
      "current_qty": 45.00,
      "reorder_point": 100.00,
      "alert_type": "LOW_STOCK",
      "is_resolved": false,
      "resolved_at": null,
      "created_at": "2026-06-29T10:00:00Z"
    },
    {
      "id": 98,
      "warehouse_id": 2,
      "warehouse_name": "Kho Hà Nội",
      "product_id": 15,
      "product_sku": "PAN-002",
      "product_name": "Chảo chống dính Sunhouse 26cm",
      "current_qty": 150.00,
      "reorder_point": 120.00,
      "alert_type": "LOW_STOCK",
      "is_resolved": true,
      "resolved_at": "2026-06-29T08:30:00Z",
      "created_at": "2026-06-28T14:00:00Z"
    }
  ]
}
```

---

## 5. Acceptance Criteria

**Scenario 1: Kích hoạt cảnh báo khi tồn kho khả dụng giảm dưới ngưỡng**
- Given sản phẩm POT-001 tại Kho Hải Phòng có định mức cảnh báo `reorder_point = 100`
- And tồn kho khả dụng hiện tại là 110 (total = 120, reserved = 10)
- When một Planner tạo đơn xuất DO mới yêu cầu 20 sản phẩm POT-001 từ Kho Hải Phòng (làm reserved tăng lên thành 30, dẫn đến tồn khả dụng giảm còn 90)
- Then hệ thống tự động ghi nhận một cảnh báo mới vào bảng `stock_alerts` với `current_qty = 90`, `reorder_point = 100`, `is_resolved = false`
- And gửi thông báo High Priority cho Trưởng kho Hải Phòng và các Planner.

**Scenario 2: Sử dụng ngưỡng cấu hình mặc định của hệ thống**
- Given sản phẩm PAN-002 tại Kho Hà Nội không cấu hình `reorder_point` (giá trị bằng null)
- And cấu hình hệ thống `MIN_INVENTORY_WARNING_THRESHOLD = 50`
- And tồn kho khả dụng hiện tại của PAN-002 là 55
- When Nhân viên kho hoàn tất lấy hàng cho một DO xuất 10 sản phẩm (làm tồn khả dụng giảm xuống còn 45)
- Then hệ thống tự động nhận diện vi phạm định mức mặc định (45 < 50)
- And tạo một cảnh báo tồn kho thấp cho sản phẩm này tại Kho Hà Nội.

**Scenario 3: Tự động giải quyết (auto-resolve) cảnh báo khi bổ sung hàng**
- Given đang có một cảnh báo tồn kho thấp chưa giải quyết (`is_resolved = false`) cho sản phẩm POT-001 tại Kho Hải Phòng (tồn khả dụng là 90, ngưỡng là 100)
- When Thủ kho hoàn tất phiếu nhập hàng từ nhà cung cấp bổ sung 50 sản phẩm POT-001 vào Kho Hải Phòng (làm tồn khả dụng tăng lên 140)
- Then hệ thống tự động cập nhật bản ghi cảnh báo hiện tại trong `stock_alerts` thành `is_resolved = true`, `resolved_at = NOW()`
- And gửi thông báo dạng thông tin (Info) xác nhận đã giải quyết xong cảnh báo thiếu hàng.

**Scenario 4: Kiểm soát phạm vi quyền hạn của Trưởng kho**
- Given Trưởng kho A được phân quyền phụ trách Kho Hải Phòng (warehouse_id = 1) và không phụ trách Kho Hà Nội (warehouse_id = 2)
- When Trưởng kho A gửi yêu cầu `GET /api/v1/alerts/low-stock?warehouse_id=2`
- Then hệ thống chặn và trả về lỗi HTTP 403 `WAREHOUSE_SCOPE_FORBIDDEN`
- When Trưởng kho A gửi yêu cầu xem cảnh báo của Kho Hải Phòng (`warehouse_id = 1`) hoặc không lọc theo kho
- Then hệ thống trả về danh sách cảnh báo của riêng Kho Hải Phòng.
