# VPS Database Schema Catalog — WMS

> **Nguồn dữ liệu:** truy vấn metadata PostgreSQL trên VPS do người dùng cung cấp (txt.rtf, txt1.rtf, txt2.rtf), đối chiếu ngày 2026-07-19. Đây là catalog vật lý chuẩn cho RDS/SDS, không suy luận từ migration hoặc entity.

## Inventory and migration status

- **56 bảng:** 55 bảng nghiệp vụ WMS + bảng kỹ thuật `flyway_schema_history`.
- **3 views chỉ đọc:** `v_inventory_by_batch`, `v_inventory_summary`, `v_low_stock_alerts`; không tính là bảng.
- **746 cột** và **227 foreign-key constraints** được xuất từ `information_schema`/`pg_constraint`.
- Flyway trên VPS có migration thành công từ V1 đến V13. **V14 chưa xuất hiện**, do đó các trường tài khoản ngân hàng dealer định nghĩa bởi V14 chưa được coi là schema production.
- Lịch sử Flyway có hai record version `22` (một SQL và một DELETE reconciliation); catalog này phản ánh đối tượng thực tế, không tự suy ra migration nào đã chạy từ version number đó.

## Conventions

- Mỗi bảng bên dưới liệt kê **toàn bộ cột** theo đúng thứ tự ordinal trên VPS. `!` = NOT NULL; `PK` = primary key; `FK→table.column` = foreign key.
- Một cột có thể có nhiều FK/constraint; các FK được gắn theo metadata, không suy đoán từ tên cột.
- Các view không có PK/FK vật lý; cột của view được liệt kê riêng.

## Complete table description and column dictionary

### `accounting_periods`

Kỳ kế toán và trạng thái đóng sổ. **PK:** `id`. **Columns (9):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('accounting_periods_id_seq'::regclass)` |
| 2 | `period_name` | `varchar` | ! | — | — |
| 3 | `start_date` | `date` | ! | — | — |
| 4 | `end_date` | `date` | ! | — | — |
| 5 | `status` | `varchar` | ! | — | `'OPEN'::character varying` |
| 6 | `closed_by` | `bigint` | ✓ | FK→users.id | — |
| 7 | `closed_at` | `timestamptz` | ✓ | — | — |
| 8 | `notes` | `text` | ✓ | — | — |
| 9 | `created_at` | `timestamptz` | ! | — | `now()` |

### `adjustments`

Phiếu điều chỉnh tồn kho có phê duyệt và liên kết nghiệp vụ nguồn. **PK:** `id`. **Columns (22):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('adjustments_id_seq'::regclass)` |
| 2 | `adjustment_number` | `varchar` | ! | — | — |
| 3 | `warehouse_id` | `bigint` | ! | FK→warehouses.id | — |
| 4 | `product_id` | `bigint` | ! | FK→products.id | — |
| 5 | `batch_id` | `bigint` | ✓ | FK→batches.id | — |
| 6 | `location_id` | `bigint` | ✓ | FK→warehouse_locations.id | — |
| 7 | `delivery_order_id` | `bigint` | ✓ | FK→delivery_orders.id | — |
| 8 | `do_item_id` | `bigint` | ✓ | FK→delivery_order_items.id | — |
| 9 | `quantity_adjustment` | `numeric` | ! | — | — |
| 10 | `type` | `varchar` | ! | — | — |
| 11 | `reference_id` | `bigint` | ✓ | — | — |
| 12 | `reference_type` | `varchar` | ✓ | — | — |
| 13 | `reason` | `text` | ! | — | — |
| 14 | `approved_by` | `bigint` | ✓ | FK→users.id | — |
| 15 | `approved_at` | `timestamptz` | ✓ | — | — |
| 16 | `document_date` | `date` | ! | — | — |
| 17 | `accounting_period_id` | `bigint` | ✓ | FK→accounting_periods.id | — |
| 18 | `created_by` | `bigint` | ! | FK→users.id | — |
| 19 | `created_at` | `timestamptz` | ! | — | `now()` |
| 20 | `quarantine_record_id` | `bigint` | ✓ | FK→quarantine_records.id | — |
| 21 | `outbound_qc_record_id` | `bigint` | ✓ | FK→outbound_qc_records.id | — |
| 22 | `allocation_id` | `bigint` | ✓ | FK→delivery_order_item_allocations.id | — |

### `audit_logs`

Nhật ký audit bất biến cho thao tác hệ thống/kho. **PK:** `id`. **Columns (12):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('audit_logs_id_seq'::regclass)` |
| 2 | `actor_id` | `bigint` | ! | FK→users.id | — |
| 3 | `actor_role` | `varchar` | ! | — | — |
| 4 | `action` | `varchar` | ! | — | — |
| 5 | `entity_type` | `varchar` | ! | — | — |
| 6 | `entity_id` | `bigint` | ! | — | — |
| 7 | `description` | `text` | ! | — | — |
| 8 | `warehouse_id` | `bigint` | ✓ | FK→warehouses.id | — |
| 9 | `old_value` | `jsonb` | ✓ | — | — |
| 10 | `new_value` | `jsonb` | ✓ | — | — |
| 11 | `timestamp` | `timestamptz` | ! | — | `now()` |
| 12 | `ip_address` | `varchar` | ✓ | — | — |

### `batches`

Lô hàng theo sản phẩm và kho. **PK:** `id`. **Columns (9):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('batches_id_seq'::regclass)` |
| 2 | `batch_number` | `varchar` | ! | — | — |
| 3 | `product_id` | `bigint` | ! | FK→products.id | — |
| 4 | `warehouse_id` | `bigint` | ! | FK→warehouses.id | — |
| 5 | `received_date` | `date` | ! | — | — |
| 6 | `quantity` | `numeric` | ! | — | — |
| 7 | `created_at` | `timestamptz` | ! | — | `now()` |
| 8 | `expiry_date` | `date` | ✓ | — | — |
| 9 | `grade` | `varchar` | ! | — | `'A'::character varying` |

### `billing_notifications`

Thông báo billing/nhắc thanh toán cho dealer. **PK:** `id`. **Columns (13):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('billing_notifications_id_seq'::regclass)` |
| 2 | `do_id` | `bigint` | ! | FK→delivery_orders.id | — |
| 3 | `do_number` | `varchar` | ! | — | — |
| 4 | `dealer_id` | `bigint` | ! | FK→dealers.id | — |
| 5 | `dealer_name` | `varchar` | ! | — | — |
| 6 | `warehouse_id` | `bigint` | ! | FK→warehouses.id | — |
| 7 | `delivered_at` | `timestamptz` | ! | — | — |
| 8 | `total_amount_estimate` | `numeric` | ! | — | — |
| 9 | `invoice_status` | `varchar` | ! | — | `'NOT_INVOICED'::character varying` |
| 10 | `status` | `varchar` | ! | — | `'ACTIVE'::character varying` |
| 11 | `recipient_role` | `varchar` | ! | — | `'ACCOUNTANT'::character varying` |
| 12 | `read_at` | `timestamptz` | ✓ | — | — |
| 13 | `created_at` | `timestamptz` | ! | — | `now()` |

### `credit_notes`

Chứng từ giảm trừ/hoàn tiền cho dealer. **PK:** `id`. **Columns (10):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('credit_notes_id_seq'::regclass)` |
| 2 | `credit_note_number` | `varchar` | ! | — | — |
| 3 | `dealer_id` | `bigint` | ! | FK→dealers.id | — |
| 4 | `receipt_id` | `bigint` | ✓ | FK→receipts.id | — |
| 5 | `amount` | `numeric` | ! | — | — |
| 6 | `reason` | `text` | ! | — | — |
| 7 | `created_by` | `bigint` | ! | FK→users.id | — |
| 8 | `document_date` | `date` | ! | — | — |
| 9 | `accounting_period_id` | `bigint` | ✓ | FK→accounting_periods.id | — |
| 10 | `created_at` | `timestamptz` | ! | — | `now()` |

### `damage_reports`

Biên bản hàng hư hỏng. **PK:** `id`. **Columns (12):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('damage_reports_id_seq'::regclass)` |
| 2 | `report_number` | `varchar` | ! | — | — |
| 3 | `warehouse_id` | `bigint` | ! | FK→warehouses.id | — |
| 4 | `product_id` | `bigint` | ! | FK→products.id | — |
| 5 | `batch_id` | `bigint` | ✓ | FK→batches.id | — |
| 6 | `quantity` | `numeric` | ! | — | — |
| 7 | `cause` | `text` | ! | — | — |
| 8 | `image_url` | `varchar` | ✓ | — | — |
| 9 | `reported_by` | `bigint` | ! | FK→users.id | — |
| 10 | `receipt_item_id` | `bigint` | ✓ | FK→receipt_items.id | — |
| 11 | `report_date` | `date` | ! | — | — |
| 12 | `created_at` | `timestamptz` | ! | — | `now()` |

### `dealers`

Khách hàng/đại lý mua hàng. **PK:** `id`. **Columns (16):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('dealers_id_seq'::regclass)` |
| 2 | `code` | `varchar` | ! | — | — |
| 3 | `name` | `varchar` | ! | — | — |
| 4 | `phone` | `varchar` | ✓ | — | — |
| 5 | `default_delivery_address` | `text` | ✓ | — | — |
| 6 | `region` | `varchar` | ✓ | — | — |
| 7 | `email` | `varchar` | ✓ | — | — |
| 8 | `payment_term_days` | `integer` | ! | — | `30` |
| 9 | `credit_limit` | `numeric` | ! | — | `0` |
| 10 | `current_balance` | `numeric` | ! | — | `0` |
| 11 | `credit_status` | `varchar` | ! | — | `'ACTIVE'::character varying` |
| 12 | `is_active` | `boolean` | ! | — | `true` |
| 13 | `created_by` | `bigint` | ✓ | FK→users.id | — |
| 14 | `updated_by` | `bigint` | ✓ | FK→users.id | — |
| 15 | `created_at` | `timestamptz` | ! | — | `now()` |
| 16 | `updated_at` | `timestamptz` | ! | — | `now()` |

### `debit_notes`

Chứng từ công nợ liên quan inbound/nhà cung cấp. **PK:** `id`. **Columns (11):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('debit_notes_id_seq'::regclass)` |
| 2 | `debit_note_number` | `varchar` | ! | — | — |
| 3 | `supplier_id` | `bigint` | ! | FK→suppliers.id | — |
| 4 | `receipt_id` | `bigint` | ✓ | FK→receipts.id | — |
| 5 | `failed_qty` | `numeric` | ! | — | — |
| 6 | `amount` | `numeric` | ! | — | — |
| 7 | `reason` | `text` | ! | — | — |
| 8 | `created_by` | `bigint` | ! | FK→users.id | — |
| 9 | `document_date` | `date` | ! | — | — |
| 10 | `accounting_period_id` | `bigint` | ✓ | FK→accounting_periods.id | — |
| 11 | `created_at` | `timestamptz` | ! | — | `now()` |

### `deliveries`

Lần giao hàng/POD cho delivery order. **PK:** `id`. **Columns (17):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('deliveries_id_seq'::regclass)` |
| 2 | `delivery_number` | `varchar` | ! | — | — |
| 3 | `do_id` | `bigint` | ! | FK→delivery_orders.id | — |
| 4 | `trip_id` | `bigint` | ✓ | FK→trips.id | — |
| 5 | `vehicle_id` | `bigint` | ! | FK→vehicles.id | — |
| 6 | `driver_id` | `bigint` | ! | FK→drivers.id | — |
| 7 | `status` | `varchar` | ! | — | `'PENDING'::character varying` |
| 8 | `pod_image_url` | `varchar` | ✓ | — | — |
| 9 | `pod_signature_url` | `varchar` | ✓ | — | — |
| 10 | `pod_timestamp` | `timestamptz` | ✓ | — | — |
| 11 | `otp_verified_at` | `timestamptz` | ✓ | — | — |
| 12 | `failure_reason` | `text` | ✓ | — | — |
| 13 | `attempt_number` | `integer` | ! | — | `1` |
| 14 | `dispatched_at` | `timestamptz` | ✓ | — | — |
| 15 | `delivered_at` | `timestamptz` | ✓ | — | — |
| 16 | `created_at` | `timestamptz` | ! | — | `now()` |
| 17 | `updated_at` | `timestamptz` | ! | — | `now()` |

### `delivery_order_approvals`

Lịch sử phê duyệt delivery order. **PK:** `id`. **Columns (7):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('delivery_order_approvals_id_seq'::regclass)` |
| 2 | `do_id` | `bigint` | ! | FK→delivery_orders.id | — |
| 3 | `approver_id` | `bigint` | ! | FK→users.id | — |
| 4 | `result` | `varchar` | ! | — | — |
| 5 | `contract_image_url` | `varchar` | ✓ | — | — |
| 6 | `rejection_reason` | `text` | ✓ | — | — |
| 7 | `approved_at` | `timestamptz` | ! | — | `now()` |

### `delivery_order_item_allocations`

Phân bổ batch/bin cho dòng xuất. **PK:** `id`. **Columns (15):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('delivery_order_item_allocations_id_seq'::regclass)` |
| 2 | `do_item_id` | `bigint` | ! | FK→delivery_order_items.id | — |
| 3 | `inventory_id` | `bigint` | ! | FK→inventories.id | — |
| 4 | `batch_id` | `bigint` | ! | FK→batches.id | — |
| 5 | `location_id` | `bigint` | ! | FK→warehouse_locations.id | — |
| 6 | `zone_id` | `bigint` | ! | FK→warehouse_locations.id | — |
| 7 | `planned_qty` | `numeric` | ! | — | — |
| 8 | `picked_qty` | `numeric` | ! | — | `0` |
| 9 | `is_replacement` | `boolean` | ! | — | `false` |
| 10 | `replaced_allocation_id` | `bigint` | ✓ | FK→delivery_order_item_allocations.id | — |
| 11 | `created_by` | `bigint` | ! | FK→users.id | — |
| 12 | `created_at` | `timestamptz` | ! | — | `now()` |
| 13 | `updated_at` | `timestamptz` | ! | — | `now()` |
| 14 | `status` | `varchar` | ! | — | `'ACTIVE'::character varying` |
| 15 | `version` | `integer` | ! | — | `0` |

### `delivery_order_item_replacements`

Thay thế dòng xuất khi outbound QC. **PK:** `id`. **Columns (12):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('delivery_order_item_replacements_id_seq'::regclass)` |
| 2 | `do_item_id` | `bigint` | ! | FK→delivery_order_items.id | — |
| 3 | `failed_inventory_id` | `bigint` | ! | FK→inventories.id | — |
| 4 | `replacement_inventory_id` | `bigint` | ! | FK→inventories.id | — |
| 5 | `failed_batch_id` | `bigint` | ! | FK→batches.id | — |
| 6 | `failed_location_id` | `bigint` | ! | FK→warehouse_locations.id | — |
| 7 | `replacement_batch_id` | `bigint` | ! | FK→batches.id | — |
| 8 | `replacement_location_id` | `bigint` | ! | FK→warehouse_locations.id | — |
| 9 | `quantity` | `numeric` | ! | — | — |
| 10 | `reason` | `text` | ! | — | — |
| 11 | `created_by` | `bigint` | ! | FK→users.id | — |
| 12 | `created_at` | `timestamptz` | ! | — | `now()` |

### `delivery_order_item_return_to_bin_records`

Trả hàng đã pick về bin. **PK:** `id`. **Columns (12):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('delivery_order_item_return_to_bin_records_id_seq'::regclass)` |
| 2 | `do_item_id` | `bigint` | ! | FK→delivery_order_items.id | — |
| 3 | `allocation_id` | `bigint` | ! | FK→delivery_order_item_allocations.id | — |
| 4 | `product_id` | `bigint` | ! | FK→products.id | — |
| 5 | `batch_id` | `bigint` | ! | FK→batches.id | — |
| 6 | `original_location_id` | `bigint` | ! | FK→warehouse_locations.id | — |
| 7 | `original_zone_id` | `bigint` | ! | FK→warehouse_locations.id | — |
| 8 | `source_location_id` | `bigint` | ✓ | FK→warehouse_locations.id | — |
| 9 | `returned_qty` | `numeric` | ! | — | — |
| 10 | `reason` | `text` | ✓ | — | — |
| 11 | `created_by` | `bigint` | ! | FK→users.id | — |
| 12 | `created_at` | `timestamptz` | ! | — | `now()` |

### `delivery_order_items`

Dòng hàng của delivery order. **PK:** `id`. **Columns (17):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('delivery_order_items_id_seq'::regclass)` |
| 2 | `do_id` | `bigint` | ! | FK→delivery_orders.id | — |
| 3 | `product_id` | `bigint` | ! | FK→products.id | — |
| 4 | `batch_id` | `bigint` | ✓ | FK→batches.id | — |
| 5 | `location_id` | `bigint` | ✓ | FK→warehouse_locations.id | — |
| 6 | `zone_id` | `bigint` | ✓ | FK→warehouse_locations.id | — |
| 7 | `requested_qty` | `numeric` | ! | — | — |
| 8 | `planned_qty` | `numeric` | ! | — | `0` |
| 9 | `picked_qty` | `numeric` | ! | — | `0` |
| 10 | `qc_pass_qty` | `numeric` | ! | — | `0` |
| 11 | `qc_fail_qty` | `numeric` | ! | — | `0` |
| 12 | `reserved_qty` | `numeric` | ! | — | `0` |
| 13 | `issued_qty` | `numeric` | ! | — | `0` |
| 14 | `unit_price` | `numeric` | ✓ | — | — |
| 15 | `unit_cost` | `numeric` | ✓ | — | — |
| 16 | `serial_number` | `varchar` | ✓ | — | — |
| 17 | `picked_by` | `bigint` | ✓ | FK→users.id | — |

### `delivery_order_warehouse_approvals`

Phê duyệt theo phạm vi kho của delivery order. **PK:** `id`. **Columns (6):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('delivery_order_warehouse_approvals_id_seq'::regclass)` |
| 2 | `do_id` | `bigint` | ! | FK→delivery_orders.id | — |
| 3 | `approver_id` | `bigint` | ! | FK→users.id | — |
| 4 | `result` | `varchar` | ! | — | — |
| 5 | `notes` | `text` | ✓ | — | — |
| 6 | `approved_at` | `timestamptz` | ! | — | `now()` |

### `delivery_orders`

Lệnh xuất/giao hàng. **PK:** `id`. **Columns (18):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('delivery_orders_id_seq'::regclass)` |
| 2 | `do_number` | `varchar` | ! | — | — |
| 3 | `dealer_id` | `bigint` | ! | FK→dealers.id | — |
| 4 | `warehouse_id` | `bigint` | ! | FK→warehouses.id | — |
| 5 | `type` | `varchar` | ! | — | — |
| 6 | `expected_delivery_date` | `date` | ✓ | — | — |
| 7 | `status` | `varchar` | ! | — | `'NEW'::character varying` |
| 8 | `created_by` | `bigint` | ! | FK→users.id | — |
| 9 | `cancel_reason` | `text` | ✓ | — | — |
| 10 | `rejection_reason` | `text` | ✓ | — | — |
| 11 | `document_date` | `date` | ! | — | — |
| 12 | `accounting_period_id` | `bigint` | ✓ | FK→accounting_periods.id | — |
| 13 | `notes` | `text` | ✓ | — | — |
| 14 | `packed_by` | `bigint` | ✓ | FK→users.id | — |
| 15 | `qc_by` | `bigint` | ✓ | FK→users.id | — |
| 16 | `created_at` | `timestamptz` | ! | — | `now()` |
| 17 | `updated_at` | `timestamptz` | ! | — | `now()` |
| 18 | `version` | `integer` | ! | — | `0` |

### `delivery_otp_attempts`

Lịch sử xác thực OTP khi giao. **PK:** `id`. **Columns (9):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('delivery_otp_attempts_id_seq'::regclass)` |
| 2 | `delivery_id` | `bigint` | ! | FK→deliveries.id | — |
| 3 | `otp_hash` | `varchar` | ! | — | — |
| 4 | `recipient_email` | `varchar` | ! | — | — |
| 5 | `expires_at` | `timestamptz` | ! | — | — |
| 6 | `consumed_at` | `timestamptz` | ✓ | — | — |
| 7 | `attempt_count` | `integer` | ! | — | `0` |
| 8 | `created_at` | `timestamptz` | ! | — | `now()` |
| 9 | `status` | `varchar` | ! | — | `'ACTIVE'::character varying` |

### `discrepancy_hold_entries`

Dòng giữ hàng do chênh lệch transfer. **PK:** `id`. **Columns (8):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('discrepancy_hold_entries_id_seq'::regclass)` |
| 2 | `incident_id` | `bigint` | ! | FK→discrepancy_incidents.id | — |
| 3 | `warehouse_id` | `bigint` | ! | FK→warehouses.id | — |
| 4 | `product_id` | `bigint` | ! | FK→products.id | — |
| 5 | `batch_id` | `bigint` | ✓ | FK→batches.id | — |
| 6 | `hold_qty` | `numeric` | ! | — | — |
| 7 | `hold_location_id` | `bigint` | ✓ | FK→warehouse_locations.id | — |
| 8 | `created_at` | `timestamptz` | ! | — | `now()` |

### `discrepancy_incidents`

Sự cố chênh lệch điều chuyển. **PK:** `id`. **Columns (11):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('discrepancy_incidents_id_seq'::regclass)` |
| 2 | `transfer_id` | `bigint` | ! | FK→inter_warehouse_transfers.id | — |
| 3 | `product_id` | `bigint` | ! | FK→products.id | — |
| 4 | `incident_type` | `varchar` | ! | — | — |
| 5 | `quantity` | `numeric` | ! | — | — |
| 6 | `status` | `varchar` | ! | — | `'OPEN'::character varying` |
| 7 | `resolution_note` | `text` | ✓ | — | — |
| 8 | `resolved_by` | `bigint` | ✓ | FK→users.id | — |
| 9 | `resolved_at` | `timestamptz` | ✓ | — | — |
| 10 | `created_at` | `timestamptz` | ! | — | `now()` |
| 11 | `updated_at` | `timestamptz` | ! | — | `now()` |

### `document_sequences`

Bộ đếm sinh số chứng từ. **PK:** `sequence_key`. **Columns (3):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `sequence_key` | `varchar` | ! | PK | — |
| 2 | `next_value` | `bigint` | ! | — | — |
| 3 | `updated_at` | `timestamptz` | ! | — | `now()` |

### `drivers`

Tài xế. **PK:** `id`. **Columns (13):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('drivers_id_seq'::regclass)` |
| 2 | `user_id` | `bigint` | ! | FK→users.id | — |
| 3 | `full_name` | `varchar` | ! | — | — |
| 4 | `phone` | `varchar` | ✓ | — | — |
| 5 | `license_number` | `varchar` | ! | — | — |
| 6 | `license_expiry` | `date` | ! | — | — |
| 7 | `warehouse_id` | `bigint` | ✓ | FK→warehouses.id | — |
| 8 | `status` | `varchar` | ! | — | `'AVAILABLE'::character varying` |
| 9 | `is_active` | `boolean` | ! | — | `true` |
| 10 | `created_by` | `bigint` | ✓ | FK→users.id | — |
| 11 | `updated_by` | `bigint` | ✓ | FK→users.id | — |
| 12 | `created_at` | `timestamptz` | ! | — | `now()` |
| 13 | `updated_at` | `timestamptz` | ! | — | `now()` |

### `flyway_schema_history`

Lịch sử migration Flyway của hệ thống. **PK:** `installed_rank`. **Columns (10):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `installed_rank` | `integer` | ! | PK | — |
| 2 | `version` | `varchar` | ✓ | — | — |
| 3 | `description` | `varchar` | ! | — | — |
| 4 | `type` | `varchar` | ! | — | — |
| 5 | `script` | `varchar` | ! | — | — |
| 6 | `checksum` | `integer` | ✓ | — | — |
| 7 | `installed_by` | `varchar` | ! | — | — |
| 8 | `installed_on` | `timestamp` | ! | — | `now()` |
| 9 | `execution_time` | `integer` | ! | — | — |
| 10 | `success` | `boolean` | ! | — | — |

### `inter_warehouse_transfer_allocations`

Phân bổ tồn cho dòng điều chuyển. **PK:** `id`. **Columns (4):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('inter_warehouse_transfer_allocations_id_seq'::regclass)` |
| 2 | `transfer_item_id` | `bigint` | ! | FK→inter_warehouse_transfer_items.id | — |
| 3 | `inventory_id` | `bigint` | ! | FK→inventories.id | — |
| 4 | `allocated_qty` | `numeric` | ! | — | — |

### `inter_warehouse_transfer_items`

Dòng hàng điều chuyển. **PK:** `id`. **Columns (20):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('transfer_items_id_seq'::regclass)` |
| 2 | `transfer_id` | `bigint` | ! | FK→inter_warehouse_transfers.id | — |
| 3 | `product_id` | `bigint` | ! | FK→products.id | — |
| 4 | `batch_id` | `bigint` | ✓ | FK→batches.id | — |
| 5 | `source_location_id` | `bigint` | ✓ | FK→warehouse_locations.id | — |
| 6 | `destination_location_id` | `bigint` | ✓ | FK→warehouse_locations.id | — |
| 7 | `planned_qty` | `numeric` | ! | — | — |
| 8 | `sent_qty` | `numeric` | ✓ | — | — |
| 9 | `received_qty` | `numeric` | ✓ | — | — |
| 10 | `variance_qty` | `numeric` | ✓ | — | — |
| 11 | `worker_received_qty` | `numeric` | ✓ | — | — |
| 12 | `qc_passed_qty` | `numeric` | ✓ | — | — |
| 13 | `qc_failed_qty` | `numeric` | ✓ | — | — |
| 14 | `qc_result` | `varchar` | ✓ | — | — |
| 15 | `qc_failure_reason` | `text` | ✓ | — | — |
| 16 | `issue_reason` | `text` | ✓ | — | — |
| 17 | `checker_note` | `text` | ✓ | — | — |
| 18 | `checked_by` | `bigint` | ✓ | FK→users.id | — |
| 19 | `checked_at` | `timestamptz` | ✓ | — | — |
| 20 | `version` | `bigint` | ! | — | `0` |

### `inter_warehouse_transfers`

Phiếu điều chuyển liên kho. **PK:** `id`. **Columns (52):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('transfers_id_seq'::regclass)` |
| 2 | `transfer_number` | `varchar` | ! | — | — |
| 3 | `external_instruction_code` | `varchar` | ! | — | `''::character varying` |
| 4 | `source_warehouse_id` | `bigint` | ! | FK→warehouses.id | — |
| 5 | `destination_warehouse_id` | `bigint` | ! | FK→warehouses.id | — |
| 6 | `trip_id` | `bigint` | ✓ | FK→trips.id | — |
| 7 | `status` | `varchar` | ! | — | `'NEW'::character varying` |
| 8 | `created_by` | `bigint` | ! | FK→users.id | — |
| 9 | `approved_by` | `bigint` | ✓ | FK→users.id | — |
| 10 | `approved_at` | `timestamptz` | ✓ | — | — |
| 11 | `confirmed_by` | `bigint` | ✓ | FK→users.id | — |
| 12 | `confirmed_at` | `timestamptz` | ✓ | — | — |
| 13 | `rejected_by` | `bigint` | ✓ | FK→users.id | — |
| 14 | `rejected_at` | `timestamptz` | ✓ | — | — |
| 15 | `rejection_reason` | `text` | ✓ | — | — |
| 16 | `planned_date` | `date` | ✓ | — | — |
| 17 | `actual_received_date` | `date` | ✓ | — | — |
| 18 | `discrepancy_reason` | `text` | ✓ | — | — |
| 19 | `is_returned` | `boolean` | ! | — | `false` |
| 20 | `notes` | `text` | ✓ | — | — |
| 21 | `document_date` | `date` | ! | — | — |
| 22 | `accounting_period_id` | `bigint` | ✓ | FK→accounting_periods.id | — |
| 23 | `created_at` | `timestamptz` | ! | — | `now()` |
| 24 | `updated_at` | `timestamptz` | ! | — | `now()` |
| 25 | `return_requested` | `boolean` | ! | — | `false` |
| 26 | `return_reason` | `text` | ✓ | — | — |
| 27 | `return_requested_by` | `bigint` | ✓ | FK→users.id | — |
| 28 | `return_requested_at` | `timestamptz` | ✓ | — | — |
| 29 | `return_approved_by` | `bigint` | ✓ | FK→users.id | — |
| 30 | `return_approved_at` | `timestamptz` | ✓ | — | — |
| 31 | `return_rejected_by` | `bigint` | ✓ | FK→users.id | — |
| 32 | `return_rejected_at` | `timestamptz` | ✓ | — | — |
| 33 | `return_rejection_reason` | `text` | ✓ | — | — |
| 34 | `transfer_request_id` | `bigint` | ✓ | FK→transfer_requests.id | — |
| 35 | `version` | `bigint` | ! | — | `0` |
| 36 | `outbound_qc_passed` | `boolean` | ✓ | — | — |
| 37 | `outbound_qc_note` | `text` | ✓ | — | — |
| 38 | `outbound_qc_photo_ref` | `text` | ✓ | — | — |
| 39 | `outbound_qc_by` | `bigint` | ✓ | FK→users.id | — |
| 40 | `outbound_qc_at` | `timestamptz` | ✓ | — | — |
| 41 | `load_handover_photo_ref` | `text` | ✓ | — | — |
| 42 | `load_handover_by` | `bigint` | ✓ | FK→users.id | — |
| 43 | `load_handover_at` | `timestamptz` | ✓ | — | — |
| 44 | `driver_arrived_at` | `timestamptz` | ✓ | — | — |
| 45 | `arrival_handover_at` | `timestamptz` | ✓ | — | — |
| 46 | `arrival_handover_by` | `bigint` | ✓ | FK→users.id | — |
| 47 | `return_departed_at` | `timestamptz` | ✓ | — | — |
| 48 | `return_arrived_at` | `timestamptz` | ✓ | — | — |
| 49 | `return_arrival_handover_at` | `timestamptz` | ✓ | — | — |
| 50 | `return_arrival_handover_by` | `bigint` | ✓ | FK→users.id | — |
| 51 | `return_photo_ref` | `text` | ✓ | — | — |
| 52 | `arrival_handover_photo_ref` | `text` | ✓ | — | — |

### `inventories`

Tồn theo kho/sản phẩm/batch/vị trí. **PK:** `id`. **Columns (10):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('inventories_id_seq'::regclass)` |
| 2 | `warehouse_id` | `bigint` | ! | FK→warehouses.id | — |
| 3 | `product_id` | `bigint` | ! | FK→products.id | — |
| 4 | `batch_id` | `bigint` | ! | FK→batches.id | — |
| 5 | `location_id` | `bigint` | ! | FK→warehouse_locations.id | — |
| 6 | `total_qty` | `numeric` | ! | — | `0` |
| 7 | `reserved_qty` | `numeric` | ! | — | `0` |
| 8 | `cost_price` | `numeric` | ! | — | — |
| 9 | `version` | `integer` | ! | — | `0` |
| 10 | `updated_at` | `timestamptz` | ! | — | `now()` |

### `invoice_lines`

Dòng hóa đơn. **PK:** `id`. **Columns (7):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('invoice_lines_id_seq'::regclass)` |
| 2 | `invoice_id` | `bigint` | ! | FK→invoices.id | — |
| 3 | `do_item_id` | `bigint` | ! | FK→delivery_order_items.id | — |
| 4 | `product_id` | `bigint` | ! | FK→products.id | — |
| 5 | `quantity` | `numeric` | ! | — | — |
| 6 | `unit_price` | `numeric` | ! | — | — |
| 7 | `line_amount` | `numeric` | ! | — | — |

### `invoices`

Hóa đơn của delivery order. **PK:** `id`. **Columns (13):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('invoices_id_seq'::regclass)` |
| 2 | `invoice_number` | `varchar` | ! | — | — |
| 3 | `do_id` | `bigint` | ! | FK→delivery_orders.id | — |
| 4 | `dealer_id` | `bigint` | ! | FK→dealers.id | — |
| 5 | `total_amount` | `numeric` | ! | — | — |
| 6 | `issue_date` | `date` | ! | — | — |
| 7 | `due_date` | `date` | ! | — | — |
| 8 | `status` | `varchar` | ! | — | `'UNPAID'::character varying` |
| 9 | `created_by` | `bigint` | ! | FK→users.id | — |
| 10 | `document_date` | `date` | ! | — | — |
| 11 | `accounting_period_id` | `bigint` | ✓ | FK→accounting_periods.id | — |
| 12 | `created_at` | `timestamptz` | ! | — | `now()` |
| 13 | `updated_at` | `timestamptz` | ! | — | `now()` |

### `notifications`

Thông báo trong hệ thống. **PK:** `id`. **Columns (8):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('notifications_id_seq'::regclass)` |
| 2 | `recipient_id` | `bigint` | ! | FK→users.id | — |
| 3 | `type` | `varchar` | ! | — | — |
| 4 | `reference_type` | `varchar` | ✓ | — | — |
| 5 | `reference_id` | `bigint` | ✓ | — | — |
| 6 | `message` | `text` | ✓ | — | — |
| 7 | `is_read` | `boolean` | ! | — | `false` |
| 8 | `created_at` | `timestamptz` | ! | — | `now()` |

### `outbound_qc_records`

Kết quả QC xuất kho. **PK:** `id`. **Columns (19):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('outbound_qc_records_id_seq'::regclass)` |
| 2 | `do_id` | `bigint` | ! | FK→delivery_orders.id | — |
| 3 | `do_item_id` | `bigint` | ! | FK→delivery_order_items.id | — |
| 4 | `allocation_id` | `bigint` | ! | FK→delivery_order_item_allocations.id | — |
| 5 | `batch_id` | `bigint` | ! | FK→batches.id | — |
| 6 | `location_id` | `bigint` | ! | FK→warehouse_locations.id | — |
| 7 | `zone_id` | `bigint` | ! | FK→warehouse_locations.id | — |
| 8 | `staging_location_id` | `bigint` | ✓ | FK→warehouse_locations.id | — |
| 9 | `quarantine_location_id` | `bigint` | ✓ | FK→warehouse_locations.id | — |
| 10 | `quarantine_record_id` | `bigint` | ✓ | FK→quarantine_records.id | — |
| 11 | `picked_qty` | `numeric` | ! | — | — |
| 12 | `qc_pass_qty` | `numeric` | ! | — | — |
| 13 | `qc_fail_qty` | `numeric` | ! | — | — |
| 14 | `qc_fail_reason` | `text` | ✓ | — | — |
| 15 | `idempotency_key` | `varchar` | ✓ | — | — |
| 16 | `request_hash` | `varchar` | ✓ | — | — |
| 17 | `notes` | `text` | ✓ | — | — |
| 18 | `created_by` | `bigint` | ! | FK→users.id | — |
| 19 | `created_at` | `timestamptz` | ! | — | `now()` |

### `payment_receipts`

Chứng từ thu tiền. **PK:** `id`. **Columns (12):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('payment_receipts_id_seq'::regclass)` |
| 2 | `payment_number` | `varchar` | ! | — | — |
| 3 | `dealer_id` | `bigint` | ! | FK→dealers.id | — |
| 4 | `invoice_id` | `bigint` | ! | FK→invoices.id | — |
| 5 | `amount` | `numeric` | ! | — | — |
| 6 | `payment_date` | `date` | ! | — | — |
| 7 | `payment_method` | `varchar` | ! | — | — |
| 8 | `created_by` | `bigint` | ! | FK→users.id | — |
| 9 | `document_date` | `date` | ! | — | — |
| 10 | `accounting_period_id` | `bigint` | ✓ | FK→accounting_periods.id | — |
| 11 | `notes` | `text` | ✓ | — | — |
| 12 | `created_at` | `timestamptz` | ! | — | `now()` |

### `price_history`

Lịch sử giá/cost theo sản phẩm. **PK:** `id`. **Columns (15):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('price_history_id_seq'::regclass)` |
| 2 | `product_id` | `bigint` | ! | FK→products.id | — |
| 3 | `warehouse_id` | `bigint` | ! | FK→warehouses.id | — |
| 4 | `effective_date` | `date` | ! | — | — |
| 6 | `cost_price` | `numeric` | ! | — | — |
| 7 | `selling_price` | `numeric` | ! | — | — |
| 8 | `status` | `varchar` | ! | — | `'PENDING'::character varying` |
| 9 | `notes` | `text` | ✓ | — | — |
| 10 | `created_by` | `bigint` | ! | FK→users.id | — |
| 11 | `approved_by` | `bigint` | ✓ | FK→users.id | — |
| 12 | `approved_at` | `timestamptz` | ✓ | — | — |
| 13 | `cancelled_by` | `bigint` | ✓ | FK→users.id | — |
| 14 | `cancelled_at` | `timestamptz` | ✓ | — | — |
| 15 | `created_at` | `timestamptz` | ! | — | `now()` |
| 16 | `updated_at` | `timestamptz` | ✓ | — | — |

### `products`

Danh mục SKU. **PK:** `id`. **Columns (18):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('products_id_seq'::regclass)` |
| 2 | `sku` | `varchar` | ! | — | — |
| 3 | `name` | `varchar` | ! | — | — |
| 4 | `unit` | `varchar` | ! | — | — |
| 5 | `unit_per_pack` | `integer` | ✓ | — | — |
| 6 | `description` | `text` | ✓ | — | — |
| 7 | `image_url` | `varchar` | ✓ | — | — |
| 8 | `weight_kg` | `numeric` | ✓ | — | — |
| 9 | `volume_m3` | `numeric` | ✓ | — | — |
| 10 | `has_expiry` | `boolean` | ! | — | `false` |
| 11 | `shelf_life_days` | `integer` | ✓ | — | — |
| 12 | `has_serial` | `boolean` | ! | — | `false` |
| 13 | `reorder_point` | `numeric` | ✓ | — | — |
| 14 | `is_active` | `boolean` | ! | — | `true` |
| 15 | `created_by` | `bigint` | ✓ | FK→users.id | — |
| 16 | `updated_by` | `bigint` | ✓ | FK→users.id | — |
| 17 | `created_at` | `timestamptz` | ! | — | `now()` |
| 18 | `updated_at` | `timestamptz` | ! | — | `now()` |

### `purchase_order_items`

Dòng purchase order. **PK:** `id`. **Columns (5):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('purchase_order_items_id_seq'::regclass)` |
| 2 | `po_id` | `bigint` | ! | FK→purchase_orders.id | — |
| 3 | `product_id` | `bigint` | ! | FK→products.id | — |
| 4 | `expected_qty` | `numeric` | ! | — | — |
| 5 | `unit_price` | `numeric` | ✓ | — | — |

### `purchase_orders`

Đơn mua từ nhà cung cấp. **PK:** `id`. **Columns (10):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('purchase_orders_id_seq'::regclass)` |
| 2 | `po_number` | `varchar` | ! | — | — |
| 3 | `supplier_id` | `bigint` | ! | FK→suppliers.id | — |
| 4 | `warehouse_id` | `bigint` | ! | FK→warehouses.id | — |
| 5 | `expected_receipt_date` | `date` | ✓ | — | — |
| 6 | `status` | `varchar` | ! | — | — |
| 7 | `created_by` | `bigint` | ! | FK→users.id | — |
| 8 | `notes` | `text` | ✓ | — | — |
| 9 | `created_at` | `timestamptz` | ! | — | `now()` |
| 10 | `updated_at` | `timestamptz` | ! | — | `now()` |

### `quarantine_records`

Hàng cách ly do QC/sự cố. **PK:** `id`. **Columns (17):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('quarantine_records_id_seq'::regclass)` |
| 2 | `warehouse_id` | `bigint` | ! | FK→warehouses.id | — |
| 3 | `product_id` | `bigint` | ! | FK→products.id | — |
| 4 | `batch_id` | `bigint` | ! | FK→batches.id | — |
| 5 | `location_id` | `bigint` | ! | FK→warehouse_locations.id | — |
| 6 | `delivery_order_id` | `bigint` | ✓ | FK→delivery_orders.id | — |
| 7 | `do_item_id` | `bigint` | ✓ | FK→delivery_order_items.id | — |
| 8 | `allocation_id` | `bigint` | ✓ | FK→delivery_order_item_allocations.id | — |
| 9 | `outbound_qc_record_id` | `bigint` | ✓ | — | — |
| 10 | `quantity` | `numeric` | ! | — | — |
| 11 | `reason` | `text` | ! | — | — |
| 12 | `created_by` | `bigint` | ! | FK→users.id | — |
| 13 | `created_at` | `timestamptz` | ! | — | `now()` |
| 14 | `transfer_id` | `bigint` | ✓ | FK→inter_warehouse_transfers.id | — |
| 15 | `transfer_item_id` | `bigint` | ✓ | FK→inter_warehouse_transfer_items.id | — |
| 16 | `origin_type` | `varchar` | ! | — | `'OUTBOUND_QC'::character varying` |
| 17 | `remaining_quantity` | `numeric` | ! | — | — |

### `receipt_items`

Dòng nhận hàng. **PK:** `id`. **Columns (20):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('receipt_items_id_seq'::regclass)` |
| 2 | `receipt_id` | `bigint` | ! | FK→receipts.id | — |
| 3 | `product_id` | `bigint` | ! | FK→products.id | — |
| 4 | `batch_id` | `bigint` | ✓ | FK→batches.id | — |
| 5 | `location_id` | `bigint` | ✓ | FK→warehouse_locations.id | — |
| 6 | `expected_qty` | `integer` | ! | — | — |
| 7 | `actual_qty` | `integer` | ✓ | — | — |
| 8 | `qc_passed_qty` | `integer` | ✓ | — | — |
| 9 | `qc_failed_qty` | `integer` | ✓ | — | — |
| 10 | `qc_result` | `varchar` | ✓ | — | — |
| 11 | `qc_failure_reason` | `text` | ✓ | — | — |
| 12 | `grade` | `varchar` | ✓ | — | — |
| 13 | `unit_cost` | `numeric` | ✓ | — | — |
| 14 | `serial_number` | `varchar` | ✓ | — | — |
| 15 | `sample_qty` | `integer` | ✓ | — | — |
| 16 | `sample_passed_qty` | `integer` | ✓ | — | — |
| 17 | `sample_failed_qty` | `integer` | ✓ | — | — |
| 18 | `over_received_qty` | `integer` | ✓ | — | — |
| 19 | `qc_sampling_method` | `varchar` | ✓ | — | — |
| 20 | `qc_by` | `bigint` | ✓ | FK→users.id | — |

### `receipts`

Phiếu nhận hàng. **PK:** `id`. **Columns (21):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('receipts_id_seq'::regclass)` |
| 2 | `receipt_number` | `varchar` | ! | — | — |
| 3 | `source_order_code` | `varchar` | ✓ | — | — |
| 4 | `type` | `varchar` | ! | — | — |
| 5 | `warehouse_id` | `bigint` | ! | FK→warehouses.id | — |
| 6 | `supplier_id` | `bigint` | ✓ | FK→suppliers.id | — |
| 7 | `dealer_id` | `bigint` | ✓ | FK→dealers.id | — |
| 8 | `contact_person` | `varchar` | ✓ | — | — |
| 9 | `source_channel` | `varchar` | ✓ | — | — |
| 10 | `status` | `varchar` | ! | — | `'PENDING_RECEIPT'::character varying` |
| 11 | `approved_by` | `bigint` | ✓ | FK→users.id | — |
| 12 | `approved_at` | `timestamptz` | ✓ | — | — |
| 13 | `rejection_reason` | `text` | ✓ | — | — |
| 14 | `document_date` | `date` | ! | — | — |
| 15 | `accounting_period_id` | `bigint` | ✓ | FK→accounting_periods.id | — |
| 16 | `created_by` | `bigint` | ! | FK→users.id | — |
| 17 | `notes` | `text` | ✓ | — | — |
| 18 | `created_at` | `timestamptz` | ! | — | `now()` |
| 19 | `updated_at` | `timestamptz` | ! | — | `now()` |
| 20 | `version` | `integer` | ! | — | `0` |
| 21 | `delivery_order_id` | `bigint` | ✓ | FK→delivery_orders.id | — |

### `stock_alerts`

Cảnh báo tồn kho. **PK:** `id`. **Columns (9):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('stock_alerts_id_seq'::regclass)` |
| 2 | `warehouse_id` | `bigint` | ! | FK→warehouses.id | — |
| 3 | `product_id` | `bigint` | ! | FK→products.id | — |
| 4 | `current_qty` | `numeric` | ! | — | — |
| 5 | `reorder_point` | `numeric` | ! | — | — |
| 6 | `alert_type` | `varchar` | ! | — | `'LOW_STOCK'::character varying` |
| 7 | `is_resolved` | `boolean` | ! | — | `false` |
| 8 | `resolved_at` | `timestamptz` | ✓ | — | — |
| 9 | `created_at` | `timestamptz` | ! | — | `now()` |

### `stock_take_items`

Dòng kiểm kê. **PK:** `id`. **Columns (10):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('stock_take_items_id_seq'::regclass)` |
| 2 | `stock_take_id` | `bigint` | ! | FK→stock_takes.id | — |
| 3 | `product_id` | `bigint` | ! | FK→products.id | — |
| 4 | `batch_id` | `bigint` | ! | FK→batches.id | — |
| 5 | `location_id` | `bigint` | ! | FK→warehouse_locations.id | — |
| 6 | `system_qty` | `numeric` | ! | — | — |
| 7 | `actual_qty` | `numeric` | ✓ | — | — |
| 8 | `variance_qty` | `numeric` | ! | — | — |
| 9 | `variance_value` | `numeric` | ! | — | — |
| 10 | `notes` | `text` | ✓ | — | — |

### `stock_takes`

Phiếu kiểm kê. **PK:** `id`. **Columns (16):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('stock_takes_id_seq'::regclass)` |
| 2 | `stock_take_number` | `varchar` | ! | — | — |
| 3 | `warehouse_id` | `bigint` | ! | FK→warehouses.id | — |
| 4 | `conducted_by` | `bigint` | ! | FK→users.id | — |
| 5 | `approved_by` | `bigint` | ✓ | FK→users.id | — |
| 6 | `approved_at` | `timestamptz` | ✓ | — | — |
| 7 | `status` | `varchar` | ! | — | `'DRAFT'::character varying` |
| 8 | `total_variance_value` | `numeric` | ✓ | — | `0` |
| 9 | `is_employee_fault` | `boolean` | ! | — | `false` |
| 10 | `approval_level` | `varchar` | ✓ | — | — |
| 11 | `rejection_reason` | `text` | ✓ | — | — |
| 12 | `stock_take_date` | `date` | ! | — | — |
| 13 | `document_date` | `date` | ! | — | — |
| 14 | `accounting_period_id` | `bigint` | ✓ | FK→accounting_periods.id | — |
| 15 | `created_at` | `timestamptz` | ! | — | `now()` |
| 16 | `updated_at` | `timestamptz` | ! | — | `now()` |

### `suppliers`

Nhà cung cấp. **PK:** `id`. **Columns (12):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('suppliers_id_seq'::regclass)` |
| 2 | `code` | `varchar` | ! | — | — |
| 3 | `company_name` | `varchar` | ! | — | — |
| 4 | `tax_code` | `varchar` | ✓ | — | — |
| 5 | `phone` | `varchar` | ✓ | — | — |
| 6 | `contact_person` | `varchar` | ✓ | — | — |
| 7 | `address` | `text` | ✓ | — | — |
| 8 | `is_active` | `boolean` | ! | — | `true` |
| 9 | `created_by` | `bigint` | ✓ | FK→users.id | — |
| 10 | `updated_by` | `bigint` | ✓ | FK→users.id | — |
| 11 | `created_at` | `timestamptz` | ! | — | `now()` |
| 12 | `updated_at` | `timestamptz` | ! | — | `now()` |

### `system_configs`

Cấu hình hệ thống. **PK:** `id`. **Columns (6):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('system_configs_id_seq'::regclass)` |
| 2 | `config_key` | `varchar` | ! | — | — |
| 3 | `config_value` | `text` | ! | — | — |
| 4 | `description` | `text` | ✓ | — | — |
| 5 | `updated_by` | `bigint` | ✓ | FK→users.id | — |
| 6 | `updated_at` | `timestamptz` | ! | — | `now()` |

### `transfer_request_items`

Dòng yêu cầu điều chuyển. **PK:** `id`. **Columns (4):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('transfer_request_items_id_seq'::regclass)` |
| 2 | `transfer_request_id` | `bigint` | ! | FK→transfer_requests.id | — |
| 3 | `product_id` | `bigint` | ! | FK→products.id | — |
| 4 | `requested_qty` | `numeric` | ! | — | — |

### `transfer_requests`

Yêu cầu điều chuyển trước khi tạo phiếu. **PK:** `id`. **Columns (22):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('transfer_requests_id_seq'::regclass)` |
| 2 | `request_number` | `varchar` | ! | — | — |
| 3 | `source_warehouse_id` | `bigint` | ! | FK→warehouses.id | — |
| 4 | `destination_warehouse_id` | `bigint` | ! | FK→warehouses.id | — |
| 5 | `status` | `varchar` | ! | — | `'DRAFT'::character varying` |
| 6 | `created_by` | `bigint` | ! | FK→users.id | — |
| 7 | `created_at` | `timestamptz` | ! | — | `now()` |
| 8 | `updated_at` | `timestamptz` | ! | — | `now()` |
| 9 | `submitted_by` | `bigint` | ✓ | FK→users.id | — |
| 10 | `submitted_at` | `timestamptz` | ✓ | — | — |
| 11 | `approved_by` | `bigint` | ✓ | FK→users.id | — |
| 12 | `approved_at` | `timestamptz` | ✓ | — | — |
| 13 | `rejected_by` | `bigint` | ✓ | FK→users.id | — |
| 14 | `rejected_at` | `timestamptz` | ✓ | — | — |
| 15 | `rejection_reason` | `text` | ✓ | — | — |
| 16 | `notes` | `text` | ✓ | — | — |
| 17 | `needed_by_date` | `date` | ✓ | — | — |
| 18 | `business_reason` | `text` | ✓ | — | — |
| 19 | `converted_transfer_id` | `bigint` | ✓ | FK→inter_warehouse_transfers.id | — |
| 20 | `converted_by` | `bigint` | ✓ | FK→users.id | — |
| 21 | `converted_at` | `timestamptz` | ✓ | — | — |
| 22 | `version` | `bigint` | ! | — | `0` |

### `trip_delivery_orders`

Bảng nối chuyến giao và delivery order. **PK:** `id`. **Columns (5):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('trip_delivery_orders_id_seq'::regclass)` |
| 2 | `trip_id` | `bigint` | ! | FK→trips.id | — |
| 3 | `do_id` | `bigint` | ! | FK→delivery_orders.id | — |
| 4 | `stop_order` | `integer` | ! | — | — |
| 5 | `is_active` | `boolean` | ! | — | `true` |

### `trips`

Chuyến giao hàng. **PK:** `id`. **Columns (21):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('trips_id_seq'::regclass)` |
| 2 | `trip_number` | `varchar` | ! | — | — |
| 3 | `vehicle_id` | `bigint` | ! | FK→vehicles.id | — |
| 4 | `driver_id` | `bigint` | ! | FK→drivers.id | — |
| 5 | `dispatcher_id` | `bigint` | ! | FK→users.id | — |
| 6 | `warehouse_id` | `bigint` | ✓ | FK→warehouses.id | — |
| 7 | `planned_date` | `date` | ! | — | — |
| 8 | `planned_start_at` | `timestamp` | ! | — | — |
| 9 | `planned_end_at` | `timestamp` | ! | — | — |
| 10 | `departed_at` | `timestamptz` | ✓ | — | — |
| 11 | `completed_at` | `timestamptz` | ✓ | — | — |
| 12 | `trip_type` | `varchar` | ! | — | `'DELIVERY'::character varying` |
| 13 | `status` | `varchar` | ! | — | `'PLANNED'::character varying` |
| 14 | `total_weight_kg` | `numeric` | ✓ | — | `0` |
| 15 | `total_volume_m3` | `numeric` | ✓ | — | `0` |
| 16 | `cancel_reason` | `text` | ✓ | — | — |
| 17 | `notes` | `text` | ✓ | — | — |
| 18 | `created_at` | `timestamptz` | ! | — | `now()` |
| 19 | `updated_at` | `timestamptz` | ! | — | `now()` |
| 20 | `calculated_weight_kg` | `numeric` | ✓ | — | — |
| 21 | `calculated_volume_m3` | `numeric` | ✓ | — | — |

### `user_warehouse_assignments`

Phân quyền/phạm vi kho của user. **PK:** `id`. **Columns (5):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('user_warehouse_assignments_id_seq'::regclass)` |
| 2 | `user_id` | `bigint` | ! | FK→users.id | — |
| 3 | `warehouse_id` | `bigint` | ! | FK→warehouses.id | — |
| 4 | `assigned_by` | `bigint` | ! | FK→users.id | — |
| 5 | `assigned_at` | `timestamptz` | ! | — | `now()` |

### `users`

Tài khoản người dùng. **PK:** `id`. **Columns (17):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('users_id_seq'::regclass)` |
| 2 | `code` | `varchar` | ! | — | — |
| 3 | `full_name` | `varchar` | ! | — | — |
| 4 | `email` | `varchar` | ! | — | — |
| 5 | `phone` | `varchar` | ✓ | — | — |
| 6 | `password_hash` | `varchar` | ! | — | — |
| 7 | `role` | `varchar` | ! | — | — |
| 8 | `job_title` | `varchar` | ✓ | — | — |
| 9 | `shift` | `varchar` | ✓ | — | — |
| 10 | `region` | `varchar` | ✓ | — | — |
| 11 | `otp_hash` | `varchar` | ✓ | — | — |
| 12 | `otp_expires_at` | `timestamptz` | ✓ | — | — |
| 13 | `refresh_token_hash` | `varchar` | ✓ | — | — |
| 14 | `refresh_token_expires_at` | `timestamptz` | ✓ | — | — |
| 15 | `is_active` | `boolean` | ! | — | `true` |
| 16 | `created_at` | `timestamptz` | ! | — | `now()` |
| 17 | `updated_at` | `timestamptz` | ! | — | `now()` |

### `vehicles`

Phương tiện vận tải. **PK:** `id`. **Columns (12):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('vehicles_id_seq'::regclass)` |
| 2 | `plate_number` | `varchar` | ! | — | — |
| 3 | `vehicle_type` | `varchar` | ! | — | — |
| 4 | `max_weight_kg` | `numeric` | ! | — | — |
| 5 | `max_volume_m3` | `numeric` | ✓ | — | — |
| 6 | `warehouse_id` | `bigint` | ! | FK→warehouses.id | — |
| 7 | `status` | `varchar` | ! | — | `'AVAILABLE'::character varying` |
| 8 | `is_active` | `boolean` | ! | — | `true` |
| 9 | `created_by` | `bigint` | ✓ | FK→users.id | — |
| 10 | `updated_by` | `bigint` | ✓ | FK→users.id | — |
| 11 | `created_at` | `timestamptz` | ! | — | `now()` |
| 12 | `updated_at` | `timestamptz` | ! | — | `now()` |

### `warehouse_locations`

Vị trí/bin trong kho. **PK:** `id`. **Columns (17):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('warehouse_locations_id_seq'::regclass)` |
| 2 | `warehouse_id` | `bigint` | ! | FK→warehouses.id | — |
| 3 | `code` | `varchar` | ! | — | — |
| 4 | `type` | `varchar` | ! | — | — |
| 5 | `parent_id` | `bigint` | ✓ | FK→warehouse_locations.id | — |
| 6 | `capacity_m3` | `numeric` | ✓ | — | — |
| 7 | `capacity_kg` | `numeric` | ✓ | — | — |
| 8 | `current_volume_m3` | `numeric` | ! | — | `0` |
| 9 | `current_weight_kg` | `numeric` | ! | — | `0` |
| 10 | `is_quarantine` | `boolean` | ! | — | `false` |
| 11 | `is_locked` | `boolean` | ! | — | `false` |
| 12 | `locked_by_stock_take_id` | `bigint` | ✓ | FK→stock_takes.id | — |
| 13 | `is_active` | `boolean` | ! | — | `true` |
| 14 | `created_by` | `bigint` | ✓ | FK→users.id | — |
| 15 | `updated_by` | `bigint` | ✓ | FK→users.id | — |
| 16 | `created_at` | `timestamptz` | ! | — | `now()` |
| 17 | `updated_at` | `timestamptz` | ! | — | `now()` |

### `warehouse_product_reservations`

Lượng tồn được giữ chỗ. **PK:** `id`. **Columns (7):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('warehouse_product_reservations_id_seq'::regclass)` |
| 2 | `warehouse_id` | `bigint` | ! | FK→warehouses.id | — |
| 3 | `product_id` | `bigint` | ! | FK→products.id | — |
| 4 | `reserved_qty` | `numeric` | ! | — | `0` |
| 5 | `version` | `integer` | ! | — | `0` |
| 6 | `created_at` | `timestamptz` | ! | — | `now()` |
| 7 | `updated_at` | `timestamptz` | ! | — | `now()` |

### `warehouses`

Kho vật lý hoặc kho in-transit. **PK:** `id`. **Columns (12):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('warehouses_id_seq'::regclass)` |
| 2 | `code` | `varchar` | ! | — | — |
| 3 | `name` | `varchar` | ! | — | — |
| 4 | `address` | `text` | ✓ | — | — |
| 5 | `phone` | `varchar` | ✓ | — | — |
| 6 | `manager_id` | `bigint` | ✓ | FK→users.id | — |
| 7 | `type` | `varchar` | ! | — | — |
| 8 | `is_active` | `boolean` | ! | — | `true` |
| 9 | `created_by` | `bigint` | ✓ | FK→users.id | — |
| 10 | `updated_by` | `bigint` | ✓ | FK→users.id | — |
| 11 | `created_at` | `timestamptz` | ! | — | `now()` |
| 12 | `updated_at` | `timestamptz` | ! | — | `now()` |

### `wrong_sku_report_items`

Dòng báo cáo sai SKU. **PK:** `id`. **Columns (9):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('wrong_sku_report_items_id_seq'::regclass)` |
| 2 | `report_id` | `bigint` | ! | FK→wrong_sku_reports.id | — |
| 3 | `transfer_item_id` | `bigint` | ✓ | FK→inter_warehouse_transfer_items.id | — |
| 4 | `expected_product_id` | `bigint` | ! | FK→products.id | — |
| 5 | `actual_product_id` | `bigint` | ! | FK→products.id | — |
| 6 | `affected_qty` | `numeric` | ! | — | — |
| 7 | `reason` | `text` | ! | — | — |
| 8 | `photo_ref` | `text` | ✓ | — | — |
| 9 | `created_at` | `timestamptz` | ! | — | `now()` |

### `wrong_sku_reports`

Báo cáo sai SKU khi outbound. **PK:** `id`. **Columns (10):**

| # | Column | PostgreSQL type | Null | Key / reference | Default |
|---:|---|---|:---:|---|---|
| 1 | `id` | `bigint` | ! | PK | `nextval('wrong_sku_reports_id_seq'::regclass)` |
| 2 | `transfer_id` | `bigint` | ! | FK→inter_warehouse_transfers.id | — |
| 3 | `status` | `varchar` | ! | — | `'PENDING'::character varying` |
| 4 | `reported_by` | `bigint` | ! | FK→users.id | — |
| 5 | `reported_at` | `timestamptz` | ! | — | `now()` |
| 6 | `manager_decision_by` | `bigint` | ✓ | FK→users.id | — |
| 7 | `manager_decision_at` | `timestamptz` | ✓ | — | — |
| 8 | `manager_note` | `text` | ✓ | — | — |
| 9 | `created_at` | `timestamptz` | ! | — | `now()` |
| 10 | `updated_at` | `timestamptz` | ! | — | `now()` |

## Read-only views

### `v_inventory_by_batch` (11 columns)

| # | Column | PostgreSQL type | Null |
|---:|---|---|:---:|
| 1 | `warehouse_code` | `varchar` | ✓ |
| 2 | `sku` | `varchar` | ✓ |
| 3 | `product_name` | `varchar` | ✓ |
| 4 | `batch_number` | `varchar` | ✓ |
| 5 | `received_date` | `date` | ✓ |
| 6 | `location_code` | `varchar` | ✓ |
| 7 | `total_qty` | `numeric` | ✓ |
| 8 | `reserved_qty` | `numeric` | ✓ |
| 9 | `available_qty` | `numeric` | ✓ |
| 10 | `cost_price` | `numeric` | ✓ |
| 11 | `line_value` | `numeric` | ✓ |

### `v_inventory_summary` (10 columns)

| # | Column | PostgreSQL type | Null |
|---:|---|---|:---:|
| 1 | `warehouse_code` | `varchar` | ✓ |
| 2 | `warehouse_name` | `varchar` | ✓ |
| 3 | `sku` | `varchar` | ✓ |
| 4 | `product_name` | `varchar` | ✓ |
| 5 | `unit` | `varchar` | ✓ |
| 6 | `total_qty` | `numeric` | ✓ |
| 7 | `reserved_qty` | `numeric` | ✓ |
| 8 | `available_qty` | `numeric` | ✓ |
| 9 | `reorder_point` | `numeric` | ✓ |
| 10 | `inventory_value` | `numeric` | ✓ |

### `v_low_stock_alerts` (8 columns)

| # | Column | PostgreSQL type | Null |
|---:|---|---|:---:|
| 1 | `alert_type` | `varchar` | ✓ |
| 2 | `warehouse_code` | `varchar` | ✓ |
| 3 | `warehouse_name` | `varchar` | ✓ |
| 4 | `sku` | `varchar` | ✓ |
| 5 | `product_name` | `varchar` | ✓ |
| 6 | `current_qty` | `numeric` | ✓ |
| 7 | `reorder_point` | `numeric` | ✓ |
| 8 | `alerted_at` | `timestamptz` | ✓ |

## Foreign-key relationship matrix

| # | Child table.column | Parent table.column | Constraint |
|---:|---|---|---|
| 1 | `accounting_periods.closed_by` | `users.id` | `accounting_periods_closed_by_fkey` |
| 2 | `adjustments.accounting_period_id` | `accounting_periods.id` | `adjustments_accounting_period_id_fkey` |
| 3 | `adjustments.allocation_id` | `delivery_order_item_allocations.id` | `adjustments_allocation_id_fkey` |
| 4 | `adjustments.approved_by` | `users.id` | `adjustments_approved_by_fkey` |
| 5 | `adjustments.batch_id` | `batches.id` | `adjustments_batch_id_fkey` |
| 6 | `adjustments.created_by` | `users.id` | `adjustments_created_by_fkey` |
| 7 | `adjustments.delivery_order_id` | `delivery_orders.id` | `adjustments_delivery_order_id_fkey` |
| 8 | `adjustments.do_item_id` | `delivery_order_items.id` | `adjustments_do_item_id_fkey` |
| 9 | `adjustments.location_id` | `warehouse_locations.id` | `adjustments_location_id_fkey` |
| 10 | `adjustments.outbound_qc_record_id` | `outbound_qc_records.id` | `adjustments_outbound_qc_record_id_fkey` |
| 11 | `adjustments.product_id` | `products.id` | `adjustments_product_id_fkey` |
| 12 | `adjustments.quarantine_record_id` | `quarantine_records.id` | `adjustments_quarantine_record_id_fkey` |
| 13 | `adjustments.warehouse_id` | `warehouses.id` | `adjustments_warehouse_id_fkey` |
| 14 | `audit_logs.actor_id` | `users.id` | `audit_logs_actor_id_fkey` |
| 15 | `audit_logs.warehouse_id` | `warehouses.id` | `audit_logs_warehouse_id_fkey` |
| 16 | `batches.product_id` | `products.id` | `batches_product_id_fkey` |
| 17 | `batches.warehouse_id` | `warehouses.id` | `batches_warehouse_id_fkey` |
| 18 | `billing_notifications.dealer_id` | `dealers.id` | `billing_notifications_dealer_id_fkey` |
| 19 | `billing_notifications.do_id` | `delivery_orders.id` | `billing_notifications_do_id_fkey` |
| 20 | `billing_notifications.warehouse_id` | `warehouses.id` | `billing_notifications_warehouse_id_fkey` |
| 21 | `credit_notes.accounting_period_id` | `accounting_periods.id` | `credit_notes_accounting_period_id_fkey` |
| 22 | `credit_notes.created_by` | `users.id` | `credit_notes_created_by_fkey` |
| 23 | `credit_notes.dealer_id` | `dealers.id` | `credit_notes_dealer_id_fkey` |
| 24 | `credit_notes.receipt_id` | `receipts.id` | `credit_notes_receipt_id_fkey` |
| 25 | `damage_reports.batch_id` | `batches.id` | `damage_reports_batch_id_fkey` |
| 26 | `damage_reports.product_id` | `products.id` | `damage_reports_product_id_fkey` |
| 27 | `damage_reports.receipt_item_id` | `receipt_items.id` | `damage_reports_receipt_item_id_fkey` |
| 28 | `damage_reports.reported_by` | `users.id` | `damage_reports_reported_by_fkey` |
| 29 | `damage_reports.warehouse_id` | `warehouses.id` | `damage_reports_warehouse_id_fkey` |
| 30 | `dealers.created_by` | `users.id` | `dealers_created_by_fkey` |
| 31 | `dealers.updated_by` | `users.id` | `dealers_updated_by_fkey` |
| 32 | `debit_notes.accounting_period_id` | `accounting_periods.id` | `debit_notes_accounting_period_id_fkey` |
| 33 | `debit_notes.created_by` | `users.id` | `debit_notes_created_by_fkey` |
| 34 | `debit_notes.receipt_id` | `receipts.id` | `debit_notes_receipt_id_fkey` |
| 35 | `debit_notes.supplier_id` | `suppliers.id` | `debit_notes_supplier_id_fkey` |
| 36 | `deliveries.do_id` | `delivery_orders.id` | `deliveries_do_id_fkey` |
| 37 | `deliveries.driver_id` | `drivers.id` | `deliveries_driver_id_fkey` |
| 38 | `deliveries.trip_id` | `trips.id` | `deliveries_trip_id_fkey` |
| 39 | `deliveries.vehicle_id` | `vehicles.id` | `deliveries_vehicle_id_fkey` |
| 40 | `delivery_order_approvals.approver_id` | `users.id` | `delivery_order_approvals_approver_id_fkey` |
| 41 | `delivery_order_approvals.do_id` | `delivery_orders.id` | `delivery_order_approvals_do_id_fkey` |
| 42 | `delivery_order_item_allocations.batch_id` | `batches.id` | `delivery_order_item_allocations_batch_id_fkey` |
| 43 | `delivery_order_item_allocations.created_by` | `users.id` | `delivery_order_item_allocations_created_by_fkey` |
| 44 | `delivery_order_item_allocations.do_item_id` | `delivery_order_items.id` | `delivery_order_item_allocations_do_item_id_fkey` |
| 45 | `delivery_order_item_allocations.inventory_id` | `inventories.id` | `delivery_order_item_allocations_inventory_id_fkey` |
| 46 | `delivery_order_item_allocations.location_id` | `warehouse_locations.id` | `delivery_order_item_allocations_location_id_fkey` |
| 47 | `delivery_order_item_allocations.replaced_allocation_id` | `delivery_order_item_allocations.id` | `delivery_order_item_allocations_replaced_allocation_id_fkey` |
| 48 | `delivery_order_item_allocations.zone_id` | `warehouse_locations.id` | `delivery_order_item_allocations_zone_id_fkey` |
| 49 | `delivery_order_item_replacements.created_by` | `users.id` | `delivery_order_item_replacements_created_by_fkey` |
| 50 | `delivery_order_item_replacements.do_item_id` | `delivery_order_items.id` | `delivery_order_item_replacements_do_item_id_fkey` |
| 51 | `delivery_order_item_replacements.failed_batch_id` | `batches.id` | `delivery_order_item_replacements_failed_batch_id_fkey` |
| 52 | `delivery_order_item_replacements.failed_inventory_id` | `inventories.id` | `delivery_order_item_replacements_failed_inventory_id_fkey` |
| 53 | `delivery_order_item_replacements.failed_location_id` | `warehouse_locations.id` | `delivery_order_item_replacements_failed_location_id_fkey` |
| 54 | `delivery_order_item_replacements.replacement_batch_id` | `batches.id` | `delivery_order_item_replacements_replacement_batch_id_fkey` |
| 55 | `delivery_order_item_replacements.replacement_inventory_id` | `inventories.id` | `delivery_order_item_replacements_replacement_inventory_id_fkey` |
| 56 | `delivery_order_item_replacements.replacement_location_id` | `warehouse_locations.id` | `delivery_order_item_replacements_replacement_location_id_fkey` |
| 57 | `delivery_order_item_return_to_bin_records.original_location_id` | `warehouse_locations.id` | `delivery_order_item_return_to_bin_rec_original_location_id_fkey` |
| 58 | `delivery_order_item_return_to_bin_records.source_location_id` | `warehouse_locations.id` | `delivery_order_item_return_to_bin_recor_source_location_id_fkey` |
| 59 | `delivery_order_item_return_to_bin_records.allocation_id` | `delivery_order_item_allocations.id` | `delivery_order_item_return_to_bin_records_allocation_id_fkey` |
| 60 | `delivery_order_item_return_to_bin_records.batch_id` | `batches.id` | `delivery_order_item_return_to_bin_records_batch_id_fkey` |
| 61 | `delivery_order_item_return_to_bin_records.created_by` | `users.id` | `delivery_order_item_return_to_bin_records_created_by_fkey` |
| 62 | `delivery_order_item_return_to_bin_records.do_item_id` | `delivery_order_items.id` | `delivery_order_item_return_to_bin_records_do_item_id_fkey` |
| 63 | `delivery_order_item_return_to_bin_records.original_zone_id` | `warehouse_locations.id` | `delivery_order_item_return_to_bin_records_original_zone_id_fkey` |
| 64 | `delivery_order_item_return_to_bin_records.product_id` | `products.id` | `delivery_order_item_return_to_bin_records_product_id_fkey` |
| 65 | `delivery_order_items.batch_id` | `batches.id` | `delivery_order_items_batch_id_fkey` |
| 66 | `delivery_order_items.do_id` | `delivery_orders.id` | `delivery_order_items_do_id_fkey` |
| 67 | `delivery_order_items.location_id` | `warehouse_locations.id` | `delivery_order_items_location_id_fkey` |
| 68 | `delivery_order_items.picked_by` | `users.id` | `delivery_order_items_picked_by_fkey` |
| 69 | `delivery_order_items.product_id` | `products.id` | `delivery_order_items_product_id_fkey` |
| 70 | `delivery_order_items.zone_id` | `warehouse_locations.id` | `delivery_order_items_zone_id_fkey` |
| 71 | `delivery_order_warehouse_approvals.approver_id` | `users.id` | `delivery_order_warehouse_approvals_approver_id_fkey` |
| 72 | `delivery_order_warehouse_approvals.do_id` | `delivery_orders.id` | `delivery_order_warehouse_approvals_do_id_fkey` |
| 73 | `delivery_orders.accounting_period_id` | `accounting_periods.id` | `delivery_orders_accounting_period_id_fkey` |
| 74 | `delivery_orders.created_by` | `users.id` | `delivery_orders_created_by_fkey` |
| 75 | `delivery_orders.dealer_id` | `dealers.id` | `delivery_orders_dealer_id_fkey` |
| 76 | `delivery_orders.packed_by` | `users.id` | `delivery_orders_packed_by_fkey` |
| 77 | `delivery_orders.qc_by` | `users.id` | `delivery_orders_qc_by_fkey` |
| 78 | `delivery_orders.warehouse_id` | `warehouses.id` | `delivery_orders_warehouse_id_fkey` |
| 79 | `delivery_otp_attempts.delivery_id` | `deliveries.id` | `delivery_otp_attempts_delivery_id_fkey` |
| 80 | `discrepancy_hold_entries.batch_id` | `batches.id` | `discrepancy_hold_entries_batch_id_fkey` |
| 81 | `discrepancy_hold_entries.hold_location_id` | `warehouse_locations.id` | `discrepancy_hold_entries_hold_location_id_fkey` |
| 82 | `discrepancy_hold_entries.incident_id` | `discrepancy_incidents.id` | `discrepancy_hold_entries_incident_id_fkey` |
| 83 | `discrepancy_hold_entries.product_id` | `products.id` | `discrepancy_hold_entries_product_id_fkey` |
| 84 | `discrepancy_hold_entries.warehouse_id` | `warehouses.id` | `discrepancy_hold_entries_warehouse_id_fkey` |
| 85 | `discrepancy_incidents.product_id` | `products.id` | `discrepancy_incidents_product_id_fkey` |
| 86 | `discrepancy_incidents.resolved_by` | `users.id` | `discrepancy_incidents_resolved_by_fkey` |
| 87 | `discrepancy_incidents.transfer_id` | `inter_warehouse_transfers.id` | `discrepancy_incidents_transfer_id_fkey` |
| 88 | `drivers.created_by` | `users.id` | `drivers_created_by_fkey` |
| 89 | `drivers.updated_by` | `users.id` | `drivers_updated_by_fkey` |
| 90 | `drivers.user_id` | `users.id` | `drivers_user_id_fkey` |
| 91 | `drivers.warehouse_id` | `warehouses.id` | `drivers_warehouse_id_fkey` |
| 92 | `inter_warehouse_transfer_allocations.inventory_id` | `inventories.id` | `inter_warehouse_transfer_allocations_inventory_id_fkey` |
| 93 | `inter_warehouse_transfer_allocations.transfer_item_id` | `inter_warehouse_transfer_items.id` | `inter_warehouse_transfer_allocations_transfer_item_id_fkey` |
| 94 | `inter_warehouse_transfer_items.batch_id` | `batches.id` | `transfer_items_batch_id_fkey` |
| 95 | `inter_warehouse_transfer_items.checked_by` | `users.id` | `transfer_items_checked_by_fkey` |
| 96 | `inter_warehouse_transfer_items.destination_location_id` | `warehouse_locations.id` | `transfer_items_destination_location_id_fkey` |
| 97 | `inter_warehouse_transfer_items.product_id` | `products.id` | `transfer_items_product_id_fkey` |
| 98 | `inter_warehouse_transfer_items.source_location_id` | `warehouse_locations.id` | `transfer_items_source_location_id_fkey` |
| 99 | `inter_warehouse_transfer_items.transfer_id` | `inter_warehouse_transfers.id` | `transfer_items_transfer_id_fkey` |
| 100 | `inter_warehouse_transfers.arrival_handover_by` | `users.id` | `inter_warehouse_transfers_arrival_handover_by_fkey` |
| 101 | `inter_warehouse_transfers.load_handover_by` | `users.id` | `inter_warehouse_transfers_load_handover_by_fkey` |
| 102 | `inter_warehouse_transfers.outbound_qc_by` | `users.id` | `inter_warehouse_transfers_outbound_qc_by_fkey` |
| 103 | `inter_warehouse_transfers.return_approved_by` | `users.id` | `inter_warehouse_transfers_return_approved_by_fkey` |
| 104 | `inter_warehouse_transfers.return_arrival_handover_by` | `users.id` | `inter_warehouse_transfers_return_arrival_handover_by_fkey` |
| 105 | `inter_warehouse_transfers.return_rejected_by` | `users.id` | `inter_warehouse_transfers_return_rejected_by_fkey` |
| 106 | `inter_warehouse_transfers.return_requested_by` | `users.id` | `inter_warehouse_transfers_return_requested_by_fkey` |
| 107 | `inter_warehouse_transfers.transfer_request_id` | `transfer_requests.id` | `inter_warehouse_transfers_transfer_request_id_fkey` |
| 108 | `inter_warehouse_transfers.accounting_period_id` | `accounting_periods.id` | `transfers_accounting_period_id_fkey` |
| 109 | `inter_warehouse_transfers.approved_by` | `users.id` | `transfers_approved_by_fkey` |
| 110 | `inter_warehouse_transfers.confirmed_by` | `users.id` | `transfers_confirmed_by_fkey` |
| 111 | `inter_warehouse_transfers.created_by` | `users.id` | `transfers_created_by_fkey` |
| 112 | `inter_warehouse_transfers.destination_warehouse_id` | `warehouses.id` | `transfers_destination_warehouse_id_fkey` |
| 113 | `inter_warehouse_transfers.rejected_by` | `users.id` | `transfers_rejected_by_fkey` |
| 114 | `inter_warehouse_transfers.source_warehouse_id` | `warehouses.id` | `transfers_source_warehouse_id_fkey` |
| 115 | `inter_warehouse_transfers.trip_id` | `trips.id` | `transfers_trip_id_fkey` |
| 116 | `inventories.batch_id` | `batches.id` | `inventories_batch_id_fkey` |
| 117 | `inventories.location_id` | `warehouse_locations.id` | `inventories_location_id_fkey` |
| 118 | `inventories.product_id` | `products.id` | `inventories_product_id_fkey` |
| 119 | `inventories.warehouse_id` | `warehouses.id` | `inventories_warehouse_id_fkey` |
| 120 | `invoice_lines.do_item_id` | `delivery_order_items.id` | `invoice_lines_do_item_id_fkey` |
| 121 | `invoice_lines.invoice_id` | `invoices.id` | `invoice_lines_invoice_id_fkey` |
| 122 | `invoice_lines.product_id` | `products.id` | `invoice_lines_product_id_fkey` |
| 123 | `invoices.accounting_period_id` | `accounting_periods.id` | `invoices_accounting_period_id_fkey` |
| 124 | `invoices.created_by` | `users.id` | `invoices_created_by_fkey` |
| 125 | `invoices.dealer_id` | `dealers.id` | `invoices_dealer_id_fkey` |
| 126 | `invoices.do_id` | `delivery_orders.id` | `invoices_do_id_fkey` |
| 127 | `notifications.recipient_id` | `users.id` | `notifications_recipient_id_fkey` |
| 128 | `outbound_qc_records.allocation_id` | `delivery_order_item_allocations.id` | `outbound_qc_records_allocation_id_fkey` |
| 129 | `outbound_qc_records.batch_id` | `batches.id` | `outbound_qc_records_batch_id_fkey` |
| 130 | `outbound_qc_records.created_by` | `users.id` | `outbound_qc_records_created_by_fkey` |
| 131 | `outbound_qc_records.do_id` | `delivery_orders.id` | `outbound_qc_records_do_id_fkey` |
| 132 | `outbound_qc_records.do_item_id` | `delivery_order_items.id` | `outbound_qc_records_do_item_id_fkey` |
| 133 | `outbound_qc_records.location_id` | `warehouse_locations.id` | `outbound_qc_records_location_id_fkey` |
| 134 | `outbound_qc_records.quarantine_location_id` | `warehouse_locations.id` | `outbound_qc_records_quarantine_location_id_fkey` |
| 135 | `outbound_qc_records.quarantine_record_id` | `quarantine_records.id` | `outbound_qc_records_quarantine_record_id_fkey` |
| 136 | `outbound_qc_records.staging_location_id` | `warehouse_locations.id` | `outbound_qc_records_staging_location_id_fkey` |
| 137 | `outbound_qc_records.zone_id` | `warehouse_locations.id` | `outbound_qc_records_zone_id_fkey` |
| 138 | `payment_receipts.accounting_period_id` | `accounting_periods.id` | `payment_receipts_accounting_period_id_fkey` |
| 139 | `payment_receipts.created_by` | `users.id` | `payment_receipts_created_by_fkey` |
| 140 | `payment_receipts.dealer_id` | `dealers.id` | `payment_receipts_dealer_id_fkey` |
| 141 | `payment_receipts.invoice_id` | `invoices.id` | `payment_receipts_invoice_id_fkey` |
| 142 | `price_history.approved_by` | `users.id` | `price_history_approved_by_fkey` |
| 143 | `price_history.cancelled_by` | `users.id` | `price_history_cancelled_by_fkey` |
| 144 | `price_history.created_by` | `users.id` | `price_history_created_by_fkey` |
| 145 | `price_history.product_id` | `products.id` | `price_history_product_id_fkey` |
| 146 | `price_history.warehouse_id` | `warehouses.id` | `price_history_warehouse_id_fkey` |
| 147 | `products.created_by` | `users.id` | `products_created_by_fkey` |
| 148 | `products.updated_by` | `users.id` | `products_updated_by_fkey` |
| 149 | `purchase_order_items.po_id` | `purchase_orders.id` | `purchase_order_items_po_id_fkey` |
| 150 | `purchase_order_items.product_id` | `products.id` | `purchase_order_items_product_id_fkey` |
| 151 | `purchase_orders.created_by` | `users.id` | `purchase_orders_created_by_fkey` |
| 152 | `purchase_orders.supplier_id` | `suppliers.id` | `purchase_orders_supplier_id_fkey` |
| 153 | `purchase_orders.warehouse_id` | `warehouses.id` | `purchase_orders_warehouse_id_fkey` |
| 154 | `quarantine_records.allocation_id` | `delivery_order_item_allocations.id` | `quarantine_records_allocation_id_fkey` |
| 155 | `quarantine_records.batch_id` | `batches.id` | `quarantine_records_batch_id_fkey` |
| 156 | `quarantine_records.created_by` | `users.id` | `quarantine_records_created_by_fkey` |
| 157 | `quarantine_records.delivery_order_id` | `delivery_orders.id` | `quarantine_records_delivery_order_id_fkey` |
| 158 | `quarantine_records.do_item_id` | `delivery_order_items.id` | `quarantine_records_do_item_id_fkey` |
| 159 | `quarantine_records.location_id` | `warehouse_locations.id` | `quarantine_records_location_id_fkey` |
| 160 | `quarantine_records.product_id` | `products.id` | `quarantine_records_product_id_fkey` |
| 161 | `quarantine_records.transfer_id` | `inter_warehouse_transfers.id` | `quarantine_records_transfer_id_fkey` |
| 162 | `quarantine_records.transfer_item_id` | `inter_warehouse_transfer_items.id` | `quarantine_records_transfer_item_id_fkey` |
| 163 | `quarantine_records.warehouse_id` | `warehouses.id` | `quarantine_records_warehouse_id_fkey` |
| 164 | `receipt_items.batch_id` | `batches.id` | `receipt_items_batch_id_fkey` |
| 165 | `receipt_items.location_id` | `warehouse_locations.id` | `receipt_items_location_id_fkey` |
| 166 | `receipt_items.product_id` | `products.id` | `receipt_items_product_id_fkey` |
| 167 | `receipt_items.qc_by` | `users.id` | `receipt_items_qc_by_fkey` |
| 168 | `receipt_items.receipt_id` | `receipts.id` | `receipt_items_receipt_id_fkey` |
| 169 | `receipts.accounting_period_id` | `accounting_periods.id` | `receipts_accounting_period_id_fkey` |
| 170 | `receipts.approved_by` | `users.id` | `receipts_approved_by_fkey` |
| 171 | `receipts.created_by` | `users.id` | `receipts_created_by_fkey` |
| 172 | `receipts.dealer_id` | `dealers.id` | `receipts_dealer_id_fkey` |
| 173 | `receipts.delivery_order_id` | `delivery_orders.id` | `receipts_delivery_order_id_fkey` |
| 174 | `receipts.supplier_id` | `suppliers.id` | `receipts_supplier_id_fkey` |
| 175 | `receipts.warehouse_id` | `warehouses.id` | `receipts_warehouse_id_fkey` |
| 176 | `stock_alerts.product_id` | `products.id` | `stock_alerts_product_id_fkey` |
| 177 | `stock_alerts.warehouse_id` | `warehouses.id` | `stock_alerts_warehouse_id_fkey` |
| 178 | `stock_take_items.batch_id` | `batches.id` | `stock_take_items_batch_id_fkey` |
| 179 | `stock_take_items.location_id` | `warehouse_locations.id` | `stock_take_items_location_id_fkey` |
| 180 | `stock_take_items.product_id` | `products.id` | `stock_take_items_product_id_fkey` |
| 181 | `stock_take_items.stock_take_id` | `stock_takes.id` | `stock_take_items_stock_take_id_fkey` |
| 182 | `stock_takes.accounting_period_id` | `accounting_periods.id` | `stock_takes_accounting_period_id_fkey` |
| 183 | `stock_takes.approved_by` | `users.id` | `stock_takes_approved_by_fkey` |
| 184 | `stock_takes.conducted_by` | `users.id` | `stock_takes_conducted_by_fkey` |
| 185 | `stock_takes.warehouse_id` | `warehouses.id` | `stock_takes_warehouse_id_fkey` |
| 186 | `suppliers.created_by` | `users.id` | `suppliers_created_by_fkey` |
| 187 | `suppliers.updated_by` | `users.id` | `suppliers_updated_by_fkey` |
| 188 | `system_configs.updated_by` | `users.id` | `system_configs_updated_by_fkey` |
| 189 | `transfer_request_items.product_id` | `products.id` | `transfer_request_items_product_id_fkey` |
| 190 | `transfer_request_items.transfer_request_id` | `transfer_requests.id` | `transfer_request_items_transfer_request_id_fkey` |
| 191 | `transfer_requests.approved_by` | `users.id` | `transfer_requests_approved_by_fkey` |
| 192 | `transfer_requests.converted_by` | `users.id` | `transfer_requests_converted_by_fkey` |
| 193 | `transfer_requests.converted_transfer_id` | `inter_warehouse_transfers.id` | `transfer_requests_converted_transfer_id_fkey` |
| 194 | `transfer_requests.created_by` | `users.id` | `transfer_requests_created_by_fkey` |
| 195 | `transfer_requests.destination_warehouse_id` | `warehouses.id` | `transfer_requests_destination_warehouse_id_fkey` |
| 196 | `transfer_requests.rejected_by` | `users.id` | `transfer_requests_rejected_by_fkey` |
| 197 | `transfer_requests.source_warehouse_id` | `warehouses.id` | `transfer_requests_source_warehouse_id_fkey` |
| 198 | `transfer_requests.submitted_by` | `users.id` | `transfer_requests_submitted_by_fkey` |
| 199 | `trip_delivery_orders.do_id` | `delivery_orders.id` | `trip_delivery_orders_do_id_fkey` |
| 200 | `trip_delivery_orders.trip_id` | `trips.id` | `trip_delivery_orders_trip_id_fkey` |
| 201 | `trips.dispatcher_id` | `users.id` | `trips_dispatcher_id_fkey` |
| 202 | `trips.driver_id` | `drivers.id` | `trips_driver_id_fkey` |
| 203 | `trips.vehicle_id` | `vehicles.id` | `trips_vehicle_id_fkey` |
| 204 | `trips.warehouse_id` | `warehouses.id` | `trips_warehouse_id_fkey` |
| 205 | `user_warehouse_assignments.assigned_by` | `users.id` | `user_warehouse_assignments_assigned_by_fkey` |
| 206 | `user_warehouse_assignments.user_id` | `users.id` | `user_warehouse_assignments_user_id_fkey` |
| 207 | `user_warehouse_assignments.warehouse_id` | `warehouses.id` | `user_warehouse_assignments_warehouse_id_fkey` |
| 208 | `vehicles.created_by` | `users.id` | `vehicles_created_by_fkey` |
| 209 | `vehicles.updated_by` | `users.id` | `vehicles_updated_by_fkey` |
| 210 | `vehicles.warehouse_id` | `warehouses.id` | `vehicles_warehouse_id_fkey` |
| 211 | `warehouse_locations.locked_by_stock_take_id` | `stock_takes.id` | `fk_wl_stock_take` |
| 212 | `warehouse_locations.created_by` | `users.id` | `warehouse_locations_created_by_fkey` |
| 213 | `warehouse_locations.parent_id` | `warehouse_locations.id` | `warehouse_locations_parent_id_fkey` |
| 214 | `warehouse_locations.updated_by` | `users.id` | `warehouse_locations_updated_by_fkey` |
| 215 | `warehouse_locations.warehouse_id` | `warehouses.id` | `warehouse_locations_warehouse_id_fkey` |
| 216 | `warehouse_product_reservations.product_id` | `products.id` | `warehouse_product_reservations_product_id_fkey` |
| 217 | `warehouse_product_reservations.warehouse_id` | `warehouses.id` | `warehouse_product_reservations_warehouse_id_fkey` |
| 218 | `warehouses.created_by` | `users.id` | `warehouses_created_by_fkey` |
| 219 | `warehouses.manager_id` | `users.id` | `warehouses_manager_id_fkey` |
| 220 | `warehouses.updated_by` | `users.id` | `warehouses_updated_by_fkey` |
| 221 | `wrong_sku_report_items.actual_product_id` | `products.id` | `wrong_sku_report_items_actual_product_id_fkey` |
| 222 | `wrong_sku_report_items.expected_product_id` | `products.id` | `wrong_sku_report_items_expected_product_id_fkey` |
| 223 | `wrong_sku_report_items.report_id` | `wrong_sku_reports.id` | `wrong_sku_report_items_report_id_fkey` |
| 224 | `wrong_sku_report_items.transfer_item_id` | `inter_warehouse_transfer_items.id` | `wrong_sku_report_items_transfer_item_id_fkey` |
| 225 | `wrong_sku_reports.manager_decision_by` | `users.id` | `wrong_sku_reports_manager_decision_by_fkey` |
| 226 | `wrong_sku_reports.reported_by` | `users.id` | `wrong_sku_reports_reported_by_fkey` |
| 227 | `wrong_sku_reports.transfer_id` | `inter_warehouse_transfers.id` | `wrong_sku_reports_transfer_id_fkey` |

## Validation notes

- Legacy physical fields such as product expiry/serial flags are present in the database export. They do **not** change the current household-goods domain policy unless a future approved migration/spec activates them.
- `inter_warehouse_transfers` and `inter_warehouse_transfer_items` are the production physical names; old `transfers` names must not be used in SQL or diagrams.
