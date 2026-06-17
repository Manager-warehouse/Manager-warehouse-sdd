# SQL Performance — WMS Phúc Anh

> Kỹ năng tối ưu truy vấn PostgreSQL cho WMS.
> Nguồn: database.md · Schema 36 bảng

## 1. Index Strategy

### Bắt buộc có index cho

| Bảng | Cột index | Lý do |
|---|---|---|
| `inventories` | `(warehouse_id, product_id, batch_id)` | Tra cứu tồn kho theo kho + SP |
| `inventories` | `(product_id, batch_id)` | FIFO selection |
| `inventory_transactions` | `(entity_type, entity_id)` | Audit lookup |
| `delivery_orders` | `(dealer_id, status)` | Dashboard & report |
| `delivery_orders` | `(trip_id)` | Tra cứu theo chuyến xe |
| `invoices` | `(dealer_id, status, due_date)` | Aging report |
| `batches` | `(product_id, received_date)` | FIFO sort |
| `price_history` | `(product_id, effective_date)` | COGS lookup |
| `audit_logs` | `(entity_type, entity_id, timestamp)` | Audit trail |
| `users` | `(email)` | Auth login |

### Composite index tips

```sql
-- FIFO: lấy batch nhập cũ nhất
CREATE INDEX idx_batches_fifo ON batches (product_id, received_date)
WHERE quantity > 0;
```

## 2. Query Patterns

### Nên làm

```sql
-- Chỉ SELECT cột cần (tránh SELECT *)
SELECT id, code, full_name FROM users WHERE email = ?;

-- Dùng JOIN thay vì N+1 queries
SELECT i.*, p.name, b.received_date
FROM inventories i
JOIN products p ON p.id = i.product_id
JOIN batches b ON b.id = i.batch_id
WHERE i.warehouse_id = ?;

-- Dùng LIMIT + OFFSET cho phân trang
SELECT * FROM delivery_orders WHERE warehouse_id = ?
ORDER BY created_at DESC LIMIT 20 OFFSET 0;
```

### Không nên

```sql
-- KHÔNG SELECT *
SELECT * FROM inventories WHERE warehouse_id = ?;

-- KHÔNG N+1 trong loop (dùng JOIN hoặc batch query)
for (order : orders) {
    order.getItems();  -- Mỗi lần gọi là 1 query riêng
}

-- KHÔNG dùng raw SQL string concatenation
-- Dùng Spring Data JPA Specification hoặc @Query với named params
```

## 3. Transaction Tips

- Dùng `@Transactional(readOnly = true)` cho GET endpoints
- Giữ transaction ngắn — không làm heavy computation trong transaction
- Pessimistic lock chỉ khi thực sự cần (`@Lock(PESSIMISTIC_WRITE)`)
- Mặc định dùng Optimistic Locking (`@Version`)

## 4. Migration Best Practices

- Mỗi migration file là 1 thay đổi — không gộp
- Test migration trên DB copy trước khi chạy production
- Index tạo trong cùng migration với table change
- Migration không được sửa sau khi đã applied

## 5. Batch Processing

```sql
-- Cập nhật batch — dùng LIMIT để tránh lock table lâu
UPDATE inventories SET quantity = quantity - ?
WHERE id IN (
    SELECT id FROM inventories
    WHERE warehouse_id = ? AND product_id = ?
    AND quantity > 0
    ORDER BY received_date ASC  -- FIFO
    LIMIT ?
);
```

## 6. Monitoring

- Enable `pg_stat_statements` để track query performance
- Log slow queries ( > 200ms ) trong application.yml
- Review query plan bằng `EXPLAIN ANALYZE` cho query chậm
