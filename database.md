## DATABASE
# 🎨 DESIGN: Hệ Thống Quản Lý Kho (WMS)

Ngày: 2026-05-24
Phiên bản: 2.0
Ghi chú: Cải thiện cho hệ thống 3 kho tại 3 khu vực

---
 35 bảng, chia 12 nhóm:

  ┌─────────────┬─────────────────────────────────────────────────────────────────────────
  │ Nhóm        │ Bảng                                                                                                 │
  ├─────────────┼─────────────────────────────────────────────────────────────────────────
  │ Hạ tầng     │ warehouses, bin_locations, vehicles, drivers                                                         │
  │ Người dùng  │ roles, users, user_warehouses                                                                        │
  │ Đối tác     │ suppliers, dealers                                                                                   │
  │ Hàng hóa    │ categories, units, products, product_units, batches, serial_numbers, price_history, promotions,      │
  │             │ promotion_products                                                                                   │
  │ Tồn kho     │ inventory, inventory_reservations                                                                    │
  │ Nhập kho    │ purchase_orders, purchase_order_items, purchase_receipts, purchase_receipt_items, return_receipts,   │
  │             │ return_receipt_items                                                                                 │
  │ Xuất kho    │ sale_orders, sale_order_items, issues, issue_items                                                   │
  │ Điều chuyển │ transfers, transfer_items                                                                            │
  │ Giao hàng   │ deliveries, delivery_attempts                                                                        │
  │ Kiểm soát   │ stock_takes, stock_take_items, adjustments, damage_reports                                           │
  │ Vận hành    │ work_shifts                                                                                          │
  │ System      │ approval_workflows, approval_requests, audit_logs, notifications, integration_queue                  │
  └─────────────┴──────────────────────────────────────────────────────────────────────────

## 1. Sơ Đồ Tổng Quan Database

  warehouses ─── users ─── roles
      │                        
  bin_locations               
      │                        
  products ─── batches ─── serial_numbers
      │
  inventory
      │
  inventory_reservations

---

## 2. Chi Tiết Từng Bảng

### 2.1 Nhóm Hạ Tầng

#### warehouses (Kho)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| code | VARCHAR(20), UNIQUE | VD: WH-HP, WH-HN, WH-HCM |
| name | VARCHAR(100) | |
| address | TEXT | |
| phone | VARCHAR(20) | |
| manager_id | UUID, FK→users | Người quản lý |
| region | VARCHAR(50) | Khu vực: miền Bắc, Trung, Nam |
| type | ENUM(physical,transit,quarantine) | |
| is_active | BOOLEAN, DEFAULT true | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

#### bin_locations (Vị trí kệ)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| warehouse_id | UUID, FK→warehouses | |
| zone | VARCHAR(10) | VD: A, B, C |
| rack | VARCHAR(10) | VD: A-01 |
| shelf | VARCHAR(10) | VD: 1 |
| bin | VARCHAR(10) | VD: 01 |
| full_code | VARCHAR(50), UNIQUE | Auto: WH-HP.A.A-01.1.01 |
| max_volume_m3 | DECIMAL(10,2) | Sức chứa m³ |
| max_weight_kg | DECIMAL(10,2) | Tải trọng kg |
| current_volume_m3 | DECIMAL(10,2), DEFAULT 0 | |
| current_weight_kg | DECIMAL(10,2), DEFAULT 0 | |
| is_active | BOOLEAN, DEFAULT true | |
| created_at | TIMESTAMP | |

#### vehicles (Xe vận chuyển)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| code | VARCHAR(20), UNIQUE | VD: XE-HP-001 |
| plate_number | VARCHAR(20) | Biển số |
| vehicle_type | VARCHAR(50) | |
| max_load_kg | DECIMAL(10,2) | |
| status | ENUM(available,in_use,maintenance) | |
| warehouse_id | UUID, FK→warehouses | |
| created_at | TIMESTAMP | |

#### drivers (Tài xế)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| code | VARCHAR(20), UNIQUE | |
| name | VARCHAR(100) | |
| phone | VARCHAR(20) | |
| license_number | VARCHAR(50) | |
| status | ENUM(available,on_trip,off_duty) | |
| warehouse_id | UUID, FK→warehouses | |
| created_at | TIMESTAMP | |

---

### 2.2 Nhóm Người Dùng & Phân Quyền

#### roles (Vai trò)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| code | VARCHAR(50), UNIQUE | admin, warehouse_manager, storekeeper, sale, accountant |
| name | VARCHAR(100) | |
| permissions | JSON | Danh sách permissions |
| is_active | BOOLEAN, DEFAULT true | |
| created_at | TIMESTAMP | |

#### users (Người dùng)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| code | VARCHAR(20), UNIQUE | Mã NV |
| name | VARCHAR(100) | |
| email | VARCHAR(100), UNIQUE | |
| phone | VARCHAR(20) | |
| password_hash | VARCHAR(255) | |
| role_id | UUID, FK→roles | |
| is_active | BOOLEAN, DEFAULT true | |
| created_at | TIMESTAMP | |

#### user_warehouses (Phân quyền kho)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| user_id | UUID, FK→users | |
| warehouse_id | UUID, FK→warehouses | |
| UNIQUE(user_id, warehouse_id) | | |

---

### 2.3 Nhóm Đối Tác

#### suppliers (Nhà cung cấp)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| code | VARCHAR(20), UNIQUE | |
| name | VARCHAR(100) | |
| phone | VARCHAR(20) | |
| address | TEXT | |
| contact_person | VARCHAR(100) | |
| tax_code | VARCHAR(50) | Mã số thuế |
| is_active | BOOLEAN, DEFAULT true | |
| created_at | TIMESTAMP | |

#### dealers (Đại lý)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| code | VARCHAR(20), UNIQUE | |
| name | VARCHAR(100) | |
| phone | VARCHAR(20) | |
| default_address | TEXT | |
| contact_person | VARCHAR(100) | |
| region | VARCHAR(50) | Khu vực giao |
| credit_limit | DECIMAL(15,2), DEFAULT 0 | Hạn mức công nợ |
| is_active | BOOLEAN, DEFAULT true | |
| created_at | TIMESTAMP | |

---

### 2.4 Nhóm Hàng Hóa - Chuẩn Hóa

#### categories (Danh mục SP)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| code | VARCHAR(20), UNIQUE | |
| name | VARCHAR(100) | |
| parent_id | UUID, FK→categories, NULL | Cha (phân cấp) |
| level | INT, DEFAULT 0 | Cấp trong cây |
| is_active | BOOLEAN, DEFAULT true | |
| created_at | TIMESTAMP | |

#### units (Đơn vị tính)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| code | VARCHAR(20), UNIQUE | VD: CAI, KG, THUNG |
| name | VARCHAR(50) | Cái, Kg, Thùng |
| conversion_factor | DECIMAL(10,4), DEFAULT 1 | Hệ số quy đổi |
| base_unit_id | UUID, FK→units, NULL | Đơn vị cơ bản |
| is_active | BOOLEAN, DEFAULT true | |
| created_at | TIMESTAMP | |

#### products (Sản phẩm)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| sku | VARCHAR(50), UNIQUE | Mã SP |
| barcode | VARCHAR(100), UNIQUE, NULL | |
| name | VARCHAR(200) | |
| category_id | UUID, FK→categories | |
| unit_id | UUID, FK→units | Đơn vị mặc định |
| description | TEXT | |
| image_url | VARCHAR(500) | |
| cost_price | DECIMAL(15,2) | Giá vốn |
| retail_price | DECIMAL(15,2) | Giá lẻ |
| dealer_price | DECIMAL(15,2) | Giá đại lý |
| reorder_point | INT, DEFAULT 0 | Tồn tối thiểu |
| reorder_quantity | INT, DEFAULT 0 | Số lượng đặt lại |
| max_stock_level | INT, DEFAULT 0 | Tồn tối đa |
| volume_m3 | DECIMAL(10,4) | Thể tích 1 đơn vị |
| weight_kg | DECIMAL(10,4) | Khối lượng 1 đơn vị |
| has_expiry | BOOLEAN, DEFAULT false | |
| has_serial | BOOLEAN, DEFAULT false | Cần tracking serial |
| is_active | BOOLEAN, DEFAULT true | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

#### product_units (Quy đổi đơn vị)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| product_id | UUID, FK→products | |
| unit_id | UUID, FK→units | |
| conversion_factor | DECIMAL(10,4) | Hệ số vs đơn vị mặc định |
| is_default | BOOLEAN, DEFAULT false | |
| UNIQUE(product_id, unit_id) | | |

#### batches (Lô hàng)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| batch_number | VARCHAR(50), UNIQUE | VD: LSP-2026-001-A |
| product_id | UUID, FK→products | |
| warehouse_id | UUID, FK→warehouses | |
| grade | ENUM(A,B,C) | 1 grade/lô |
| received_date | DATE | |
| expiry_date | DATE, NULL | NULL nếu không có hạn |
| initial_quantity | INT | Số lượng ban đầu |
| current_quantity | INT | Số lượng hiện tại |
| status | ENUM(active,depleted,expired,quarantine) | |
| created_at | TIMESTAMP | |

#### serial_numbers (Số serial)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| product_id | UUID, FK→products | |
| batch_id | UUID, FK→batches | |
| serial_number | VARCHAR(100), UNIQUE | |
| status | ENUM(available,reserved,sold,damaged,returned) | |
| warehouse_id | UUID, FK→warehouses, NULL | |
| bin_location_id | UUID, FK→bin_locations, NULL | |
| created_at | TIMESTAMP | |

#### price_history (Lịch sử giá)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| product_id | UUID, FK→products | |
| price_type | ENUM(cost,retail,dealer) | |
| old_price | DECIMAL(15,2) | |
| new_price | DECIMAL(15,2) | |
| changed_by | UUID, FK→users | |
| changed_at | TIMESTAMP | |

#### promotions (Khuyến mãi)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| code | VARCHAR(20), UNIQUE | |
| name | VARCHAR(200) | |
| discount_type | ENUM(percentage,fixed_amount) | |
| discount_value | DECIMAL(15,2) | |
| min_order_value | DECIMAL(15,2), DEFAULT 0 | |
| start_date | DATE | |
| end_date | DATE | |
| is_active | BOOLEAN, DEFAULT true | |
| created_at | TIMESTAMP | |

#### promotion_products (SP áp dụng KM)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| promotion_id | UUID, FK→promotions | |
| product_id | UUID, FK→products | |
| UNIQUE(promotion_id, product_id) | | |

---

### 2.5 Nhóm Tồn Kho

#### inventory (Tồn kho)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| warehouse_id | UUID, FK→warehouses | |
| product_id | UUID, FK→products | |
| batch_id | UUID, FK→batches, NULL | NULL = không batch |
| bin_location_id | UUID, FK→bin_locations, NULL | Vị trí cụ thể |
| quantity | INT, DEFAULT 0, CHECK >= 0 | Tồn thực tế |
| reserved_quantity | INT, DEFAULT 0 | Đã giữ cho đơn |
| cost_price | DECIMAL(15,2) | Giá vốn |
| version | INT, DEFAULT 0 | Optimistic locking |
| updated_at | TIMESTAMP | |

> **Unique**: (warehouse_id, product_id, batch_id, bin_location_id)
> batch NULL: chỉ 1 row cho warehouse+product+bin không batch

#### inventory_reservations (Giữ hàng)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| inventory_id | UUID, FK→inventory | |
| reference_type | VARCHAR(50) | sale_orders, transfers |
| reference_id | UUID | ID đơn hàng |
| quantity | INT | Số lượng giữ |
| status | ENUM(pending,allocated,released,fulfilled) | |
| expires_at | TIMESTAMP, NULL | Hết hạn giữ |
| created_at | TIMESTAMP | |

---

### 2.6 Nhóm Nhập Kho

#### purchase_orders (Đơn đặt hàng NCC)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| po_number | VARCHAR(30), UNIQUE | VD: PO-2026-001 |
| supplier_id | UUID, FK→suppliers | |
| warehouse_id | UUID, FK→warehouses | Kho nhận |
| expected_date | DATE | |
| status | ENUM(draft,confirmed,receiving,completed,cancelled) | |
| notes | TEXT | |
| created_by | UUID, FK→users | |
| confirmed_by | UUID, FK→users, NULL | |
| confirmed_at | TIMESTAMP, NULL | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

#### purchase_order_items (Chi tiết PO)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| po_id | UUID, FK→purchase_orders | |
| product_id | UUID, FK→products | |
| unit_id | UUID, FK→units | |
| quantity_ordered | INT | |
| quantity_received | INT, DEFAULT 0 | |
| quantity_cancelled | INT, DEFAULT 0 | |
| unit_price | DECIMAL(15,2) | |

#### purchase_receipts (Phiếu nhập NCC)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| receipt_number | VARCHAR(30), UNIQUE | VD: NK-2026-001 |
| po_id | UUID, FK→purchase_orders, NULL | |
| warehouse_id | UUID, FK→warehouses | |
| supplier_id | UUID, FK→suppliers | |
| received_by | UUID, FK→users | |
| delivered_by | VARCHAR(100) | |
| received_at | TIMESTAMP | |
| status | ENUM(draft,qc_pending,qc_done,completed,cancelled) | |
| notes | TEXT | |
| created_at | TIMESTAMP | |

#### purchase_receipt_items (Chi tiết nhập NCC)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| receipt_id | UUID, FK→purchase_receipts | |
| product_id | UUID, FK→products | |
| po_item_id | UUID, FK→purchase_order_items, NULL | |
| batch_id | UUID, FK→batches, NULL | |
| bin_location_id | UUID, FK→bin_locations, NULL | |
| quantity | INT | |
| unit_price | DECIMAL(15,2) | |
| qc_status | ENUM(pending,passed,failed) | |
| qc_notes | TEXT | |
| quarantine_quantity | INT, DEFAULT 0 | |

#### return_receipts (Phiếu nhập hoàn hàng)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| receipt_number | VARCHAR(30), UNIQUE | VD: TH-2026-001 |
| dealer_id | UUID, FK→dealers | |
| warehouse_id | UUID, FK→warehouses | |
| reason | ENUM(defect,expired,wrong_item,customer_return,other) | |
| received_by | UUID, FK→users | |
| received_at | TIMESTAMP | |
| status | ENUM(draft,qc_pending,qc_done,completed,cancelled) | |
| notes | TEXT | |
| created_at | TIMESTAMP | |

#### return_receipt_items (Chi tiết hoàn hàng)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| receipt_id | UUID, FK→return_receipts | |
| product_id | UUID, FK→products | |
| original_issue_id | UUID, FK→issues, NULL | |
| batch_id | UUID, FK→batches, NULL | |
| bin_location_id | UUID, FK→bin_locations, NULL | |
| quantity | INT | |
| condition | ENUM(resale,damaged,disposed) | |
| qc_status | ENUM(pending,passed,failed) | |
| qc_notes | TEXT | |

---

### 2.7 Nhóm Xuất Kho & Đơn Sale

#### sale_orders (Đơn hàng Sale)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| order_number | VARCHAR(30), UNIQUE | VD: SO-2026-001 |
| dealer_id | UUID, FK→dealers | |
| warehouse_id | UUID, FK→warehouses | Gợi ý kho xuất |
| delivery_address | TEXT | |
| expected_delivery | DATE | |
| total_amount | DECIMAL(15,2) | |
| discount_amount | DECIMAL(15,2), DEFAULT 0 | |
| promotion_id | UUID, FK→promotions, NULL | |
| status | ENUM(pending,confirmed,preparing,allocated,issued,delivered,completed,cancelled) | |
| notes | TEXT | |
| created_by | UUID, FK→users | |
| confirmed_by | UUID, FK→users, NULL | |
| confirmed_at | TIMESTAMP, NULL | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

#### sale_order_items (Chi tiết đơn Sale)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| sale_order_id | UUID, FK→sale_orders | |
| product_id | UUID, FK→products | |
| unit_id | UUID, FK→units | |
| quantity | INT | |
| quantity_allocated | INT, DEFAULT 0 | Đã giữ |
| quantity_issued | INT, DEFAULT 0 | Đã xuất |
| unit_price | DECIMAL(15,2) | |
| discount_amount | DECIMAL(15,2), DEFAULT 0 | |

#### issues (Phiếu xuất kho)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| issue_number | VARCHAR(30), UNIQUE | VD: XK-2026-001 |
| type | ENUM(dealer,retail,internal,transfer) | |
| warehouse_id | UUID, FK→warehouses | Kho xuất |
| sale_order_id | UUID, FK→sale_orders, NULL | |
| transfer_id | UUID, FK→transfers, NULL | |
| dealer_id | UUID, FK→dealers, NULL | |
| status | ENUM(draft,approved,picking,picked,completed,cancelled) | |
| issued_by | UUID, FK→users | |
| approved_by | UUID, FK→users, NULL | |
| picked_by | UUID, FK→users, NULL | Người chọn hàng |
| picked_at | TIMESTAMP, NULL | |
| notes | TEXT | |
| created_at | TIMESTAMP | |

#### issue_items (Chi tiết phiếu xuất)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| issue_id | UUID, FK→issues | |
| product_id | UUID, FK→products | |
| batch_id | UUID, FK→batches, NULL | FEFO/FIFO |
| bin_location_id | UUID, FK→bin_locations, NULL | |
| quantity | INT | |
| quantity_picked | INT, DEFAULT 0 | Đã chọn thực tế |
| unit_cost | DECIMAL(15,2) | |

---

### 2.8 Nhóm Điều Chuyển

#### transfers (Phiếu điều chuyển)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| transfer_number | VARCHAR(30), UNIQUE | VD: DC-2026-001 |
| source_warehouse_id | UUID, FK→warehouses | Kho nguồn |
| dest_warehouse_id | UUID, FK→warehouses | Kho đích |
| status | ENUM(draft,approved,in_transit,received,completed,cancelled) | |
| reason | TEXT | |
| created_by | UUID, FK→users | |
| approved_by | UUID, FK→users, NULL | |
| shipped_at | TIMESTAMP, NULL | |
| received_by | UUID, FK→users, NULL | |
| received_at | TIMESTAMP, NULL | |
| received_notes | TEXT | |
| created_at | TIMESTAMP | |

#### transfer_items (Chi tiết điều chuyển)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| transfer_id | UUID, FK→transfers | |
| product_id | UUID, FK→products | |
| batch_id | UUID, FK→batches, NULL | |
| quantity_sent | INT | |
| quantity_received | INT, DEFAULT 0 | |
| variance_reason | TEXT, NULL | |

---

### 2.9 Nhóm Giao Hàng

#### deliveries (Vận đơn)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| delivery_number | VARCHAR(30), UNIQUE | |
| issue_id | UUID, FK→issues | |
| vehicle_id | UUID, FK→vehicles, NULL | |
| driver_id | UUID, FK→drivers, NULL | |
| recipient_name | VARCHAR(100) | Người nhận |
| recipient_phone | VARCHAR(20) | |
| delivery_address | TEXT | |
| delivery_notes | TEXT | VD: gọi trước 30 phút |
| status | ENUM(pending,in_transit,attempt_failed,delivered,completed,cancelled) | |
| attempt_count | INT, DEFAULT 0 | |
| failure_reason | TEXT, NULL | |
| delivered_at | TIMESTAMP, NULL | |
| signature_url | VARCHAR(500), NULL | Chữ ký POD |
| photo_urls | JSON, NULL | |
| gps_location | VARCHAR(100), NULL | |
| created_at | TIMESTAMP | |

#### delivery_attempts (Lịch sử giao)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| delivery_id | UUID, FK→deliveries | |
| attempt_number | INT | |
| attempted_at | TIMESTAMP | |
| status | ENUM(success,failed) | |
| failure_reason | TEXT, NULL | |
| gps_location | VARCHAR(100), NULL | |
| notes | TEXT | |

---

### 2.10 Nhóm Kiểm Soát

#### stock_takes (Phiếu kiểm kê)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| stocktake_number | VARCHAR(30), UNIQUE | |
| warehouse_id | UUID, FK→warehouses | |
| type | ENUM(monthly,adhoc,annual) | |
| status | ENUM(draft,in_progress,completed,approved) | |
| started_at | TIMESTAMP | |
| performed_by | UUID, FK→users | |
| approved_by | UUID, FK→users, NULL | |
| approved_at | TIMESTAMP, NULL | |
| completed_at | TIMESTAMP, NULL | |
| notes | TEXT | |
| created_at | TIMESTAMP | |

#### stock_take_items (Chi tiết kiểm kê)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| stock_take_id | UUID, FK→stock_takes | |
| product_id | UUID, FK→products | |
| batch_id | UUID, FK→batches, NULL | |
| bin_location_id | UUID, FK→bin_locations, NULL | |
| system_quantity | INT | Tồn hệ thống |
| actual_quantity | INT | Tồn thực tế |
| variance | INT | Auto tính |
| counted_by | UUID, FK→users | |
| counted_at | TIMESTAMP | |
| verified | BOOLEAN, DEFAULT false | |
| notes | TEXT | |

#### adjustments (Phiếu điều chỉnh)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| adjustment_number | VARCHAR(30), UNIQUE | |
| stock_take_id | UUID, FK→stock_takes, NULL | |
| warehouse_id | UUID, FK→warehouses | |
| product_id | UUID, FK→products | |
| batch_id | UUID, FK→batches, NULL | |
| bin_location_id | UUID, FK→bin_locations, NULL | |
| quantity_before | INT | |
| quantity_after | INT | |
| reason | TEXT | |
| adjustment_type | ENUM(stock_take,damage,found,write_off,other) | |
| status | ENUM(pending,approved,rejected) | |
| created_by | UUID, FK→users | |
| approved_by | UUID, FK→users, NULL | |
| created_at | TIMESTAMP | |

#### damage_reports (Báo cáo hư hỏng)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| report_number | VARCHAR(30), UNIQUE | |
| warehouse_id | UUID, FK→warehouses | |
| product_id | UUID, FK→products | |
| batch_id | UUID, FK→batches, NULL | |
| bin_location_id | UUID, FK→bin_locations, NULL | |
| quantity | INT | |
| cause | ENUM(transport,storage,handling,expired,unknown) | |
| description | TEXT | |
| photo_urls | JSON, NULL | |
| reported_by | UUID, FK→users | |
| status | ENUM(reported,investigating,resolved) | |
| resolved_notes | TEXT, NULL | |
| resolved_at | TIMESTAMP, NULL | |
| created_at | TIMESTAMP | |

---

### 2.11 Nhóm Vận Hành

#### work_shifts (Ca làm việc)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| user_id | UUID, FK→users | |
| warehouse_id | UUID, FK→warehouses | |
| shift_date | DATE | |
| shift_type | ENUM(morning,afternoon,night) | |
| check_in | TIMESTAMP | |
| check_out | TIMESTAMP, NULL | |
| orders_processed | INT, DEFAULT 0 | |
| items_processed | INT, DEFAULT 0 | |
| created_at | TIMESTAMP | |

---

### 2.12 Nhóm System

#### approval_workflows (Quy trình duyệt)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| name | VARCHAR(100) | |
| reference_type | VARCHAR(50) | purchase_orders, transfers, adjustments |
| approval_levels | JSON | [{"level": 1, "role": "warehouse_manager"}] |
| auto_approve_threshold | DECIMAL(15,2), NULL | Tự duyệt dưới ngưỡng |
| is_active | BOOLEAN, DEFAULT true | |
| created_at | TIMESTAMP | |

#### approval_requests (Yêu cầu duyệt)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| workflow_id | UUID, FK→approval_workflows | |
| reference_type | VARCHAR(50) | |
| reference_id | UUID | |
| current_level | INT, DEFAULT 1 | |
| status | ENUM(pending,approved,rejected) | |
| rejected_reason | TEXT, NULL | |
| created_by | UUID, FK→users | |
| created_at | TIMESTAMP | |
| resolved_by | UUID, FK→users, NULL | |
| resolved_at | TIMESTAMP, NULL | |

#### audit_logs (Nhật ký hệ thống)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| user_id | UUID, FK→users | |
| action | VARCHAR(50) | CREATE, UPDATE, DELETE |
| table_name | VARCHAR(50) | |
| record_id | UUID | |
| old_data | JSON, NULL | |
| new_data | JSON, NULL | |
| ip_address | VARCHAR(50) | |
| user_agent | VARCHAR(500) | |
| created_at | TIMESTAMP | |

#### notifications (Thông báo)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| user_id | UUID, FK→users | |
| type | VARCHAR(50) | reorder_alert, order_new, approval_needed... |
| title | VARCHAR(200) | |
| message | TEXT | |
| priority | ENUM(low,medium,high) | |
| is_read | BOOLEAN, DEFAULT false | |
| read_at | TIMESTAMP, NULL | |
| reference_type | VARCHAR(50), NULL | |
| reference_id | UUID, NULL | |
| created_at | TIMESTAMP | |

#### integration_queue (Hàng đợi liên thông)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID, PK | |
| target_system | ENUM(accounting,hrm,sale) | |
| event_type | VARCHAR(50) | |
| payload | JSON | |
| status | ENUM(pending,sent,failed,retry) | |
| retry_count | INT, DEFAULT 0 | |
| max_retries | INT, DEFAULT 3 | |
| error_message | TEXT, NULL | |
| created_at | TIMESTAMP | |
| sent_at | TIMESTAMP, NULL | |

---

## 3. Sơ Đồ Quan Hệ (ER Diagram)

  warehouses ─── bin_locations
      │                  │
      │                  └─── inventory ◄── products ◄── categories
      │                                        │
      │                        batches ◄───────┘
      │                            │
      │              inventory_reservations ◄── sale_orders
      │
      ├── vehicles ◄── deliveries ◄── delivery_attempts
      │
      ├── drivers
      │
      ├── transfers ◄── transfer_items
      │
      └── stock_takes ◄── stock_take_items ◄── adjustments

  users ── roles
  user_warehouses ── warehouses

  suppliers ── purchase_orders ── purchase_receipts
  dealers ── sale_orders ── issues
  promotions ── promotion_products ── products

---

## 4. Index Quan Trọng

| Bảng | Index | Mục đích |
|------|-------|---------|
| inventory | (warehouse_id, product_id) | Tra tồn nhanh |
| inventory | (product_id, batch_id) | Tra lô |
| batches | (product_id, expiry_date) | FEFO |
| batches | (product_id, received_date) | FIFO |
| batches | (warehouse_id, status) | Batch theo kho |
| sale_orders | (status, created_at) | Đơn chờ |
| sale_orders | (dealer_id, created_at) | Đơn đại lý |
| issues | (warehouse_id, status) | Xuất kho |
| issues | (sale_order_id) | Tra từ đơn Sale |
| purchase_receipts | (warehouse_id, status, created_at) | Nhập kho |
| return_receipts | (dealer_id, status) | Hoàn hàng |
| transfers | (source_warehouse_id, status) | Điều chuyển ra |
| transfers | (dest_warehouse_id, status) | Điều chuyển vào |
| deliveries | (status, created_at) | Vận đơn |
| audit_logs | (table_name, record_id) | Tra thay đổi |
| audit_logs | (user_id, created_at) | Log người |
| notifications | (user_id, is_read, created_at) | Thông báo |
| integration_queue | (status, created_at) | Hàng đợi |
| products | (sku) | Tra SKU |
| products | (barcode) | Quét barcode |
| products | (category_id) | Lọc danh mục |
| batches | (status, expiry_date) | Hết hạn |
| inventory_reservations | (reference_type, reference_id) | Tra giữ hàng |

---

## 5. Quy Tắc Nghiệp Vụ

### 5.1 Tồn kho
1. CHECK: inventory.quantity >= 0
2. quantity >= reserved_quantity luôn đúng
3. FEFO: chọn batch expiry gần nhất còn hạn
4. FIFO: chọn batch received_date cũ nhất
5. Optimistic locking: kiểm tra version trước UPDATE
6. Điều chỉnh: chỉ qua adjustments, không sửa trực tiếp inventory

### 5.2 Batch & Grade
1. 1 Grade/Lô: mỗi batch 1 grade, khác grade phải tạo batch mới
2. Putaway: kiểm tra bin_capacity trước khi đặt
3. Serial: sản phẩm has_serial=true phải nhập serial khi nhập/xuất

### 5.3 Đơn hàng
1. sale_order.status=confirmed → tạo inventory_reservation
2. Hết hạn giữ → auto release
3. Partial allocation: cho phép giao 1 phần

### 5.4 Điều chuyển
1. status=approved → giảm tồn nguồn
2. status=received → tăng tồn đích
3. quantity_sent != quantity_received → tạo adjustment

### 5.5 Duyệt duyệt
1. Dưới ngưỡng auto_approve_threshold → tự duyệt
2. Mỗi level cần duyệt bởi role tương ứng
3. Reject: ghi rejected_reason, quay lại sửa

### 5.6 Soft delete
- Master data: is_active = false
- Transaction data: status = cancelled
- Không xóa vĩnh viễn

---

## 6. Chức Năng Chính

### 6.1 Quản lý kho
- CRUD kho vật lý (3 kho)
- Quản lý vị trí kệ (zone/rack/shelf/bin)
- Theo dõi sức chứa kho

### 6.2 Quản lý hàng hóa
- CRUD sản phẩm (SKU, barcode, hình ảnh)
- Phân loại sản phẩm (danh mục cây)
- Quản lý đơn vị tính + quy đổi
- Quản lý batch (FEFO/FIFO)
- Tracking serial number (optional)
- Lịch sử giá

### 6.3 Nhập kho
- Tạo và duyệt PO
- Nhận hàng NCC
- QC (pass/fail/quarantine)
- Hoàn hàng từ đại lý
- Putaway: chọn vị trí kệ

### 6.4 Xuất kho
- Tạo đơn từ Sale
- Duyệt đơn hàng
- Xác nhận tồn kho (allocation)
- Tạo phiếu xuất kho
- Picking: chọn batch FEFO

### 6.5 Điều chuyển
- Tạo phiếu điều chuyển giữa 3 kho
- Duyệt điều chuyển
- Theo dõi in_transit
- Xác nhận nhận hàng + ghi chú chênh lệch

### 6.6 Giao hàng
- Tạo vận đơn từ phiếu xuất
- Gán tài xế + xe
- Theo dõi GPS
- Nhiều lần giao thử
- Chụp ảnh + chữ ký POD

### 6.7 Kiểm kê
- Tạo phiếu kiểm kê (định kỳ/tháng/năm)
- Nhập số đếm thực tế
- Auto tính chênh lệch
- Duyệt và tạo điều chỉnh

### 6.8 Báo cáo
- Tồn kho theo kho/sản phẩm
- Hàng sắp hết hạn
- Hàng dưới điểm tái đặt
- Đơn hàng theo trạng thái
- Hiệu suất giao hàng

### 6.9 Cảnh báo
- Reorder: tồn dưới reorder_point
- Expiry: batch sắp hết hạn (30 ngày)
- QC fail
- Delivery fail

---