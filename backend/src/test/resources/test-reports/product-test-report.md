# Báo cáo Kiểm thử — Feature: Quản lý SKU & Danh mục Sản phẩm (US-WMS-19)

**Ngày thực hiện:** 2026-06-04  
**Nhánh:** `product-merge`  
**Người thực hiện:** Đặng Đức Dương  
**Kết quả tổng thể:** ✅ 32/32 PASSED

---

## 1. Phạm vi kiểm thử

| File test | Loại | Số test |
|-----------|------|---------|
| `ProductServiceTest.java` | Unit Test (Mockito) | 14 |
| `ProductControllerTest.java` | Integration Test (MockMvc) | 18 |

---

## 2. Kết quả chi tiết — ProductServiceTest (Unit Test)

| # | Tên test | Mô tả | Kết quả |
|---|----------|-------|---------|
| 1 | `getProducts_noSearch_returnsPage` | Không có search → trả về page sản phẩm đúng | ✅ PASS |
| 2 | `getProducts_withSearch_returnsFiltered` | Có search param → trả về kết quả lọc | ✅ PASS |
| 3 | `getProduct_exists_returnsResponse` | ID tồn tại → trả về ProductResponse đầy đủ | ✅ PASS |
| 4 | `getProduct_notFound_throwsException` | ID không tồn tại → throw `PRODUCT_NOT_FOUND` | ✅ PASS |
| 5 | `createProduct_validSku_createsProduct` | SKU hợp lệ → tạo thành công, `is_active=true` | ✅ PASS |
| 6 | `createProduct_duplicateSku_throwsException` | SKU trùng → throw `DUPLICATE_SKU`, không gọi `save()` | ✅ PASS |
| 7 | `createProduct_hasExpiry_savesShelfLifeDays` | `has_expiry=true` → lưu đúng `shelf_life_days` | ✅ PASS |
| 8 | `createProduct_hasSerial_savedCorrectly` | `has_serial=true` → lưu đúng | ✅ PASS |
| 9 | `createProduct_withUnitPerPack_savedCorrectly` | Có `unit_per_pack` → lưu đúng quy đổi thùng→cái | ✅ PASS |
| 10 | `updateProduct_valid_updatesSuccessfully` | Cập nhật hợp lệ → gọi `save()` và trả về response | ✅ PASS |
| 11 | `updateProduct_duplicateSkuOtherProduct_throwsException` | SKU trùng với sản phẩm khác → throw `DUPLICATE_SKU` | ✅ PASS |
| 12 | `updateProduct_notFound_throwsException` | ID không tồn tại → throw `PRODUCT_NOT_FOUND` | ✅ PASS |
| 13 | `deactivateProduct_exists_setsInactive` | Sản phẩm tồn tại → set `is_active=false` và gọi `save()` | ✅ PASS |
| 14 | `deactivateProduct_notFound_throwsException` | ID không tồn tại → throw `PRODUCT_NOT_FOUND` | ✅ PASS |

---

## 3. Kết quả chi tiết — ProductControllerTest (Integration Test)

| # | Endpoint | Tên test | Mô tả | HTTP | Kết quả |
|---|----------|----------|-------|------|---------|
| 1 | `GET /products` | `getProducts_storekeeper_returns200` | STOREKEEPER → 200 + data | 200 | ✅ PASS |
| 2 | `GET /products` | `getProducts_ceo_returns200` | CEO (và mọi role đã đăng nhập) → 200 | 200 | ✅ PASS |
| 3 | `GET /products` | `getProducts_unauthenticated_returns403` | Không có token → 403 | 403 | ✅ PASS |
| 4 | `GET /products` | `getProducts_withSearch_returns200` | Có `?search=SKU` → 200 + kết quả lọc | 200 | ✅ PASS |
| 5 | `GET /products/{id}` | `getProduct_exists_returns200` | ID tồn tại → 200 + đầy đủ fields | 200 | ✅ PASS |
| 6 | `GET /products/{id}` | `getProduct_notFound_returns404` | ID không tồn tại → 404 `PRODUCT_NOT_FOUND` | 404 | ✅ PASS |
| 7 | `POST /products` | `createProduct_storekeeper_returns201` | STOREKEEPER, body hợp lệ → 201 | 201 | ✅ PASS |
| 8 | `POST /products` | `createProduct_ceo_returns403` | CEO tạo sản phẩm → 403 | 403 | ✅ PASS |
| 9 | `POST /products` | `createProduct_duplicateSku_returns409` | SKU trùng → 409 `DUPLICATE_SKU` | 409 | ✅ PASS |
| 10 | `POST /products` | `createProduct_missingSku_returns400` | Thiếu `sku` → 400 validation error | 400 | ✅ PASS |
| 11 | `POST /products` | `createProduct_missingName_returns400` | Thiếu `name` → 400 validation error | 400 | ✅ PASS |
| 12 | `PUT /products/{id}` | `updateProduct_storekeeper_returns200` | STOREKEEPER, body hợp lệ → 200 | 200 | ✅ PASS |
| 13 | `PUT /products/{id}` | `updateProduct_planner_returns403` | PLANNER cập nhật → 403 | 403 | ✅ PASS |
| 14 | `PUT /products/{id}` | `updateProduct_notFound_returns404` | ID không tồn tại → 404 `PRODUCT_NOT_FOUND` | 404 | ✅ PASS |
| 15 | `DELETE /products/{id}` | `deactivateProduct_storekeeper_returns204` | STOREKEEPER soft-delete → 204 | 204 | ✅ PASS |
| 16 | `DELETE /products/{id}` | `deactivateProduct_admin_returns204` | ADMIN soft-delete → 204 | 204 | ✅ PASS |
| 17 | `DELETE /products/{id}` | `deactivateProduct_ceo_returns403` | CEO xóa sản phẩm → 403 | 403 | ✅ PASS |
| 18 | `DELETE /products/{id}` | `deactivateProduct_notFound_returns404` | ID không tồn tại → 404 `PRODUCT_NOT_FOUND` | 404 | ✅ PASS |

---

## 4. Lưu ý kỹ thuật

| Vấn đề | Giải thích |
|--------|-----------|
| `DELETE` là soft-delete | Endpoint `DELETE /products/{id}` chỉ set `is_active=false`, không xóa row khỏi DB. Hành vi đúng theo spec. |
| `GET /products` cho phép tất cả role | Chỉ `POST`, `PUT`, `DELETE` mới bị giới hạn role `STOREKEEPER` và `ADMIN`. |
| `DUPLICATE_SKU` trả 409 Conflict | Đã thêm case vào `GlobalExceptionHandler`. |
| `PRODUCT_NOT_FOUND` trả 404 | Xử lý qua `ResourceNotFoundException` đã có sẵn trong project. |
| Unit conversion `thùng → cái` | Lưu qua field `unit_per_pack`, không có logic tính toán thêm ở tầng Service trong Sprint 1. |

---

## 5. Các file đã thêm / chỉnh sửa

| Hành động | File | Mô tả |
|-----------|------|-------|
| ✏️ Sửa | `backend/src/main/java/com/wms/entity/Product.java` | Thêm Lombok annotations (`@Getter`, `@Setter`, `@Builder`, ...), `@PrePersist`, `@PreUpdate` tự set `createdAt`, `updatedAt`, default `isActive=true` |
| ➕ Thêm | `backend/src/main/java/com/wms/repository/ProductRepository.java` | Interface JPA: `existsBySku`, `existsBySkuAndIdNot`, `findAllBySearch` (JPQL search theo SKU/name) |
| ➕ Thêm | `backend/src/main/java/com/wms/dto/request/ProductRequest.java` | Request DTO với Jakarta Validation (`@NotBlank`, `@Size`, `@NotNull`) |
| ➕ Thêm | `backend/src/main/java/com/wms/dto/response/ProductResponse.java` | Response DTO với `@Builder` |
| ➕ Thêm | `backend/src/main/java/com/wms/service/ProductService.java` | Interface: `getProducts`, `getProduct`, `createProduct`, `updateProduct`, `deactivateProduct` |
| ➕ Thêm | `backend/src/main/java/com/wms/service/impl/ProductServiceImpl.java` | Business logic: check DUPLICATE_SKU, soft-delete, map entity ↔ DTO |
| ➕ Thêm | `backend/src/main/java/com/wms/controller/ProductController.java` | 5 endpoints đúng spec, phân quyền `@PreAuthorize` cho STOREKEEPER/ADMIN |
| ✏️ Sửa | `backend/src/main/java/com/wms/exception/GlobalExceptionHandler.java` | Thêm handler cho `ResourceNotFoundException` (404) và case `DUPLICATE_SKU` (409) |
| ➕ Thêm | `backend/src/test/java/com/wms/service/ProductServiceTest.java` | 14 unit tests (Mockito) |
| ➕ Thêm | `backend/src/test/java/com/wms/controller/ProductControllerTest.java` | 18 integration tests (MockMvc) |

---

## 6. Tóm tắt

```
ProductServiceTest    (Unit):    14 passed, 0 failed
ProductControllerTest (MockMvc): 18 passed, 0 failed
──────────────────────────────────────────────────
TOTAL (Product):                 32 passed, 0 failed  ✅
```
