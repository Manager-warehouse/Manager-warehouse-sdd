# Integration Tests — WMS Phúc Anh

> Hướng dẫn viết Integration Test cho toàn bộ API endpoints.
> Nguồn: constitution.md · database.md

## 1. Mục tiêu

- Test toàn bộ REST API endpoints (happy + error paths)
- Test DB interaction qua Spring Data JPA
- Test transaction boundaries và rollback
- Test audit log được tạo đúng
- Test business rules enforcement ở mức integration

## 2. Cấu trúc

```
backend/src/test/java/com/wms/
├── controller/     # @WebMvcTest — test controller layer
└── integration/    # @SpringBootTest — full integration test
```

## 3. Cấu hình

### application-test.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/wms_test
    username: wms_test
    password: wms_test
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  flyway:
    enabled: false
```

Dùng Testcontainers PostgreSQL cho CI:

```java
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReceiptApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
        .withDatabaseName("wms_test")
        .withUsername("wms_test")
        .withPassword("wms_test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

## 4. Test Coverage Matrix

Mỗi endpoint phải test:

| Loại test | Ví dụ |
|---|---|
| Happy path (201/200) | Tạo receipt thành công |
| Validation error (400) | Thiếu field bắt buộc, quantity <= 0 |
| Auth error (401) | Không gửi JWT token |
| Forbidden (403) | User không có quyền |
| Not found (404) | warehouse_id không tồn tại |
| Conflict (409) | Version conflict khi update inventory |
| Business rule (422) | Xuất quá tồn kho, xuất khi Dealer đang CREDIT_HOLD |

## 5. Ví dụ Controller Integration Test

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ReceiptControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ReceiptRepository receiptRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private ObjectMapper objectMapper;

    private String jwtToken;

    @BeforeEach
    void setUp() {
        // Login -> lấy token
        jwtToken = loginAsPlanner();
        receiptRepository.deleteAll();
        inventoryRepository.deleteAll();
    }

    @Test
    void createReceipt_validRequest_shouldReturn201() throws Exception {
        // Given
        CreateReceiptRequest request = new CreateReceiptRequest();
        request.setWarehouseId(1L);
        request.setItems(List.of(
            new ReceiptItemDto(1L, 100, 1L)
        ));

        // When / Then
        mockMvc.perform(post("/api/v1/receipts")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.code").isString())
            .andExpect(jsonPath("$.data.status").value("PENDING_RECEIPT"));
    }

    @Test
    void createReceipt_negativeQuantity_shouldReturn400() throws Exception {
        CreateReceiptRequest request = new CreateReceiptRequest();
        request.setWarehouseId(1L);
        request.setItems(List.of(
            new ReceiptItemDto(1L, -5, 1L)  // quantity âm
        ));

        mockMvc.perform(post("/api/v1/receipts")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createReceipt_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/receipts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }
}
```

## 6. API Endpoints cần test

| Resource | HTTP | Endpoint | Priority |
|---|---|---|---|
| Auth | POST | `/api/v1/auth/login` | P1 |
| Auth | POST | `/api/v1/auth/refresh` | P1 |
| Warehouses | GET | `/api/v1/warehouses` | P1 |
| Products | CRUD | `/api/v1/products` | P1 |
| Receipts | CRUD | `/api/v1/receipts` | P1 |
| Receipts | PUT | `/api/v1/receipts/{id}/approve` | P1 |
| Receipts | PUT | `/api/v1/receipts/{id}/reject` | P1 |
| Receipts | PUT | `/api/v1/receipts/{id}/return-to-supplier/confirm` | P1 |
| Receipts | POST | `/api/v1/receipts/{id}/rtv` | P1 |
| Receipts | PUT | `/api/v1/receipts/{id}/rtv/confirm` | P1 |
| Delivery Orders | CRUD | `/api/v1/delivery-orders` | P1 |
| Delivery Orders | PATCH | `/api/v1/delivery-orders/{id}/status` | P1 |
| Transfers | CRUD | `/api/v1/transfers` | P1 |
| Inventory | GET | `/api/v1/warehouse-stock` | P1 |
| Stocktakes | CRUD | `/api/v1/stocktakes` | P1 |
| Invoices | CRUD | `/api/v1/invoices` | P1 |
| Payments | POST | `/api/v1/payment-receipts` | P1 |
| Dealers | CRUD | `/api/v1/dealers` | P1 |
| Dashboard | GET | `/api/v1/dashboard/*` | P1 |

## 7. Coverage Check

```bash
# Chạy integration tests
mvn test -Dtest="*IntegrationTest"

# Check coverage
mvn jacoco:report
# Mở target/site/jacoco/index.html
```
