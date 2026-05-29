# Data Model: Core WMS Operations — Sprint 1

## Entity-Relationship Overview

```
Warehouse (1) ----< Inventory (n) >---- Product (1)
                  Inventory (n) >---- Batch (1)
                  Inventory (n) >---- BinLocation (1)

Receipt (1) ----< ReceiptItem (n) >---- Product
                  ReceiptItem (n) >---- Batch
                  
DeliveryOrder (1) ----< DeliveryOrderItem (n) >---- Product
                        DeliveryOrderItem (n) >---- Batch

TransferOrder (1) ----< TransferOrderItem (n) >---- Product
                        TransferOrderItem (n) >---- Batch

StockTake (1) ----< StockTakeItem (n) >---- Product
                   StockTakeItem (n) >---- BinLocation

User (1) >---- Role (n)
User (1) >---- Warehouse (n)  [via user_warehouse]

Dealer (1) ----< DeliveryOrder (n)
Dealer (1) ----< Invoice (n)
Invoice (1) ----< Payment (n)

Trip (1) ----< DeliveryOrder (n)
Trip (1) >---- User (driver)

AuditLog ---- [polymorphic: references any entity]
```

---

## Entity Definitions

### 1. Warehouse
| Field | Type | Constraints |
|-------|------|-------------|
| id | BIGINT | PK, auto-increment |
| code | VARCHAR(20) | UNIQUE, NOT NULL (e.g., "HP", "HN", "HCM") |
| name | VARCHAR(100) | NOT NULL |
| address | TEXT | |
| is_active | BOOLEAN | DEFAULT true, soft delete |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |
| version | INTEGER | @Version, optimistic locking |

**Indexes**: (code) UNIQUE, (is_active)

### 2. Product (SKU)
| Field | Type | Constraints |
|-------|------|-------------|
| id | BIGINT | PK, auto-increment |
| sku | VARCHAR(50) | UNIQUE, NOT NULL |
| name | VARCHAR(200) | NOT NULL |
| category | VARCHAR(100) | |
| unit | VARCHAR(20) | NOT NULL (e.g., "thung", "cai", "kg") |
| has_serial | BOOLEAN | DEFAULT false |
| has_expiry | BOOLEAN | DEFAULT false |
| is_active | BOOLEAN | DEFAULT true, soft delete |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |
| version | INTEGER | @Version |

**Indexes**: (sku) UNIQUE, (category), (is_active)

### 3. Batch (Lot)
| Field | Type | Constraints |
|-------|------|-------------|
| id | BIGINT | PK, auto-increment |
| product_id | BIGINT | FK -> Product, NOT NULL |
| batch_code | VARCHAR(50) | NOT NULL |
| grade | VARCHAR(1) | NOT NULL, CHECK IN ('A','B','C') |
| expiry_date | DATE | NULL if !has_expiry |
| received_date | DATE | NOT NULL |
| is_expired | BOOLEAN | DEFAULT false |
| notes | TEXT | |
| created_at | TIMESTAMP | |
| version | INTEGER | @Version |

**Indexes**: (product_id, batch_code) UNIQUE, (product_id, expiry_date), (product_id, received_date)
**Constraints**: Moi batch chi co 1 grade (CHECK). Khac grade = tao batch moi.

### 4. BinLocation
| Field | Type | Constraints |
|-------|------|-------------|
| id | BIGINT | PK, auto-increment |
| warehouse_id | BIGINT | FK -> Warehouse, NOT NULL |
| zone | VARCHAR(20) | (e.g., "A", "B", "C", "QUARANTINE") |
| aisle | VARCHAR(20) | |
| rack | VARCHAR(20) | |
| level | VARCHAR(10) | |
| bin_code | VARCHAR(50) | NOT NULL (generated: WH-ZONE-AISLE-RACK-LEVEL) |
| capacity_qty | DECIMAL(15,3) | NOT NULL |
| occupied_qty | DECIMAL(15,3) | DEFAULT 0 |
| is_active | BOOLEAN | DEFAULT true |
| version | INTEGER | @Version |

**Indexes**: (warehouse_id, bin_code) UNIQUE, (warehouse_id, zone)

### 5. Inventory
| Field | Type | Constraints |
|-------|------|-------------|
| id | BIGINT | PK, auto-increment |
| warehouse_id | BIGINT | FK -> Warehouse, NOT NULL |
| product_id | BIGINT | FK -> Product, NOT NULL |
| batch_id | BIGINT | FK -> Batch, NOT NULL |
| bin_location_id | BIGINT | FK -> BinLocation |
| quantity | DECIMAL(15,3) | NOT NULL, CHECK >= 0 |
| reserved_qty | DECIMAL(15,3) | DEFAULT 0, CHECK >= 0 |
| is_quarantine | BOOLEAN | DEFAULT false |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |
| version | INTEGER | @Version, optimistic locking |

**Indexes**: 
- (warehouse_id, product_id, batch_id) UNIQUE (one inventory record per batch+product+warehouse)
- (warehouse_id, is_quarantine)
- (batch_id, warehouse_id)
**Constraints**: `quantity >= 0`, `reserved_qty >= 0`, `reserved_qty <= quantity`

### 6. Receipt
| Field | Type | Constraints |
|-------|------|-------------|
| id | BIGINT | PK, auto-increment |
| receipt_code | VARCHAR(50) | UNIQUE, NOT NULL |
| warehouse_id | BIGINT | FK -> Warehouse, NOT NULL |
| created_by | BIGINT | FK -> User, NOT NULL |
| status | VARCHAR(20) | NOT NULL: PENDING -> QC_IN_PROGRESS -> APPROVED -> COMPLETED / REJECTED |
| total_items | INT | |
| notes | TEXT | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |
| version | INTEGER | @Version |

**Indexes**: (receipt_code) UNIQUE, (warehouse_id, status), (created_by)

### 7. ReceiptItem
| Field | Type | Constraints |
|-------|------|-------------|
| id | BIGINT | PK, auto-increment |
| receipt_id | BIGINT | FK -> Receipt, NOT NULL |
| product_id | BIGINT | FK -> Product, NOT NULL |
| batch_id | BIGINT | FK -> Batch, NULL (set during QC pass) |
| expected_qty | DECIMAL(15,3) | NOT NULL |
| received_qty | DECIMAL(15,3) | |
| qc_passed_qty | DECIMAL(15,3) | |
| qc_failed_qty | DECIMAL(15,3) | |
| qc_status | VARCHAR(20) | PENDING -> PASSED / FAILED / PARTIAL |
| qc_notes | TEXT | |
| bin_location_id | BIGINT | FK -> BinLocation |

### 8. DeliveryOrder
| Field | Type | Constraints |
|-------|------|-------------|
| id | BIGINT | PK, auto-increment |
| do_code | VARCHAR(50) | UNIQUE, NOT NULL |
| warehouse_id | BIGINT | FK -> Warehouse, NOT NULL |
| dealer_id | BIGINT | FK -> Dealer, NOT NULL |
| created_by | BIGINT | FK -> User, NOT NULL |
| status | VARCHAR(30) | DRAFT -> CREDIT_CHECK -> PICKING -> QC_OUTBOUND -> READY_TO_SHIP -> IN_TRANSIT -> DELIVERED / CANCELLED |
| total_amount | DECIMAL(15,2) | |
| notes | TEXT | |
| credit_check_status | VARCHAR(20) | PASSED / HOLD |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |
| version | INTEGER | @Version |

**Indexes**: (do_code) UNIQUE, (warehouse_id, status), (dealer_id, status)

### 9. DeliveryOrderItem
| Field | Type | Constraints |
|-------|------|-------------|
| id | BIGINT | PK, auto-increment |
| delivery_order_id | BIGINT | FK -> DeliveryOrder, NOT NULL |
| product_id | BIGINT | FK -> Product, NOT NULL |
| batch_id | BIGINT | FK -> Batch |
| quantity | DECIMAL(15,3) | NOT NULL |
| picked_qty | DECIMAL(15,3) | DEFAULT 0 |
| price | DECIMAL(15,2) | |
| subtotal | DECIMAL(15,2) | |

### 10. TransferOrder
| Field | Type | Constraints |
|-------|------|-------------|
| id | BIGINT | PK, auto-increment |
| transfer_code | VARCHAR(50) | UNIQUE, NOT NULL |
| source_warehouse_id | BIGINT | FK -> Warehouse, NOT NULL |
| dest_warehouse_id | BIGINT | FK -> Warehouse, NOT NULL |
| created_by | BIGINT | FK -> User, NOT NULL |
| approved_by | BIGINT | FK -> User, NULL |
| status | VARCHAR(30) | DRAFT -> PENDING_APPROVAL -> APPROVED -> IN_TRANSIT -> PARTIALLY_RECEIVED -> COMPLETED / CANCELLED |
| quantity_sent | DECIMAL(15,3) | |
| quantity_received | DECIMAL(15,3) | |
| notes | TEXT | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |
| version | INTEGER | @Version |

**Indexes**: (transfer_code) UNIQUE, (source_warehouse_id, status), (dest_warehouse_id, status)

### 11. TransferOrderItem
| Field | Type | Constraints |
|-------|------|-------------|
| id | BIGINT | PK, auto-increment |
| transfer_order_id | BIGINT | FK -> TransferOrder, NOT NULL |
| product_id | BIGINT | FK -> Product, NOT NULL |
| batch_id | BIGINT | FK -> Batch |
| quantity_sent | DECIMAL(15,3) | NOT NULL |
| quantity_received | DECIMAL(15,3) | |

### 12. StockTake
| Field | Type | Constraints |
|-------|------|-------------|
| id | BIGINT | PK, auto-increment |
| stocktake_code | VARCHAR(50) | UNIQUE, NOT NULL |
| warehouse_id | BIGINT | FK -> Warehouse, NOT NULL |
| created_by | BIGINT | FK -> User, NOT NULL |
| approved_by | BIGINT | FK -> User, NULL |
| status | VARCHAR(30) | DRAFT -> IN_PROGRESS -> COMPLETED -> APPROVED -> ADJUSTED / REJECTED |
| total_variance_amount | DECIMAL(15,2) | |
| notes | TEXT | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |
| version | INTEGER | @Version |

**Indexes**: (stocktake_code) UNIQUE, (warehouse_id, status)

### 13. StockTakeItem
| Field | Type | Constraints |
|-------|------|-------------|
| id | BIGINT | PK, auto-increment |
| stocktake_id | BIGINT | FK -> StockTake, NOT NULL |
| product_id | BIGINT | FK -> Product, NOT NULL |
| bin_location_id | BIGINT | FK -> BinLocation |
| system_qty | DECIMAL(15,3) | NOT NULL |
| actual_qty | DECIMAL(15,3) | |
| variance_qty | DECIMAL(15,3) | |
| variance_amount | DECIMAL(15,2) | |
| notes | TEXT | |

### 14. User
| Field | Type | Constraints |
|-------|------|-------------|
| id | BIGINT | PK, auto-increment |
| username | VARCHAR(50) | UNIQUE, NOT NULL |
| password_hash | VARCHAR(255) | NOT NULL (bcrypt) |
| full_name | VARCHAR(100) | NOT NULL |
| email | VARCHAR(100) | |
| is_active | BOOLEAN | DEFAULT true |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |
| version | INTEGER | @Version |

### 15. Role
| Field | Type | Constraints |
|-------|------|-------------|
| id | BIGINT | PK, auto-increment |
| name | VARCHAR(30) | UNIQUE, NOT NULL (CEO, SYSTEM_ADMIN, WAREHOUSE_MANAGER, ACCOUNTANT_MANAGER, PLANNER, DISPATCHER, STORE_KEEPER, WAREHOUSE_STAFF, ACCOUNTANT, DRIVER) |
| description | VARCHAR(200) | |

### 16. UserRole (join table)
| Field | Type | Constraints |
|-------|------|-------------|
| user_id | BIGINT | FK -> User |
| role_id | BIGINT | FK -> Role |
| warehouse_id | BIGINT | FK -> Warehouse, NULL (null = all warehouses) |

**Indexes**: (user_id, role_id, warehouse_id) UNIQUE

### 17. Dealer
| Field | Type | Constraints |
|-------|------|-------------|
| id | BIGINT | PK, auto-increment |
| code | VARCHAR(30) | UNIQUE, NOT NULL |
| name | VARCHAR(200) | NOT NULL |
| credit_limit | DECIMAL(15,2) | NOT NULL |
| current_balance | DECIMAL(15,2) | DEFAULT 0 |
| credit_status | VARCHAR(20) | DEFAULT 'ACTIVE' (ACTIVE / CREDIT_HOLD) |
| payment_terms | VARCHAR(10) | 'NET30' / 'NET60' |
| is_active | BOOLEAN | DEFAULT true |
| version | INTEGER | @Version |

### 18. Invoice
| Field | Type | Constraints |
|-------|------|-------------|
| id | BIGINT | PK, auto-increment |
| invoice_code | VARCHAR(50) | UNIQUE, NOT NULL |
| dealer_id | BIGINT | FK -> Dealer, NOT NULL |
| delivery_order_id | BIGINT | FK -> DeliveryOrder, NOT NULL |
| total_amount | DECIMAL(15,2) | NOT NULL |
| status | VARCHAR(20) | PENDING / PAID / OVERDUE / CANCELLED |
| due_date | DATE | |
| created_at | TIMESTAMP | |

### 19. Payment
| Field | Type | Constraints |
|-------|------|-------------|
| id | BIGINT | PK, auto-increment |
| invoice_id | BIGINT | FK -> Invoice |
| dealer_id | BIGINT | FK -> Dealer |
| amount | DECIMAL(15,2) | NOT NULL |
| payment_date | TIMESTAMP | NOT NULL |
| reference | VARCHAR(100) | |
| notes | TEXT | |

### 20. Trip
| Field | Type | Constraints |
|-------|------|-------------|
| id | BIGINT | PK, auto-increment |
| trip_code | VARCHAR(50) | UNIQUE, NOT NULL |
| warehouse_id | BIGINT | FK -> Warehouse, NOT NULL |
| driver_id | BIGINT | FK -> User (DRIVER), NOT NULL |
| vehicle_plate | VARCHAR(20) | |
| status | VARCHAR(30) | PLANNED -> LOADING -> IN_TRANSIT -> OUT_FOR_DELIVERY -> COMPLETED / RETURNED |
| stop_order | TEXT | JSON array of delivery order IDs in stop order |
| departed_at | TIMESTAMP | |
| completed_at | TIMESTAMP | |
| notes | TEXT | |

### 21. AuditLog
| Field | Type | Constraints |
|-------|------|-------------|
| id | BIGINT | PK, auto-increment |
| actor_id | BIGINT | FK -> User, NOT NULL |
| action | VARCHAR(50) | NOT NULL (e.g., "RECEIPT_CREATED", "INVENTORY_ADJUSTED") |
| entity_type | VARCHAR(50) | NOT NULL |
| entity_id | BIGINT | NOT NULL |
| before_state | JSONB | |
| after_state | JSONB | |
| details | TEXT | |
| ip_address | VARCHAR(45) | |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

**Indexes**: (actor_id), (entity_type, entity_id), (action), (created_at)

---

## State Machine Diagrams

### Receipt Status Flow
```
PENDING -> QC_IN_PROGRESS -> APPROVED -> COMPLETED
                                  \-> REJECTED
```

### DeliveryOrder Status Flow
```
DRAFT -> CREDIT_CHECK -> PICKING -> QC_OUTBOUND -> READY_TO_SHIP -> IN_TRANSIT -> DELIVERED
                                                                    \-> CANCELLED (at any stage)
```

### TransferOrder Status Flow
```
DRAFT -> PENDING_APPROVAL -> APPROVED -> IN_TRANSIT -> PARTIALLY_RECEIVED -> COMPLETED
                                                                  \-> CANCELLED
```

### StockTake Status Flow
```
DRAFT -> IN_PROGRESS -> COMPLETED -> APPROVED -> ADJUSTED
                                        \-> REJECTED
```

---

## Key Validation Rules

1. **Inventory**: `quantity >= 0`, `reserved_qty >= 0`, `reserved_qty <= quantity`
2. **BinLocation**: `occupied_qty <= capacity_qty`
3. **Batch**: Moi batch chi co 1 grade (A/B/C)
4. **Batch expiry**: Batch het han khong duoc chon cho DeliveryOrder
5. **QC**: Quantity passed + failed = received_qty
6. **Transfer**: `source_warehouse_id != dest_warehouse_id`
7. **Credit Check**: `current_balance + DO_amount <= credit_limit`
8. **Dealer credit_status**: Tu dong set CREDIT_HOLD khi vuot limit hoac overdue > 30 ngay
