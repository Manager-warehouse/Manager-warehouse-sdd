# Unit Tests — WMS Phúc Anh

> Hướng dẫn viết Unit Test cho backend (JUnit 5 + Mockito) và frontend (Jest).
> Nguồn: constitution.md (Section 7) · AGENTS.md (Testing)

## 1. Backend — JUnit 5 + Mockito

### Cấu trúc

```
backend/src/test/java/com/wms/
├── service/        # Unit test cho services
├── controller/     # Unit test cho controllers (mock service)
├── util/           # Unit test cho utilities (FIFO)
└── repository/     # Repository test với @DataJpaTest (integration)
```

### Coverage yêu cầu

| Layer | Coverage | Bắt buộc |
|---|---|---|
| Service (business logic) | ≥ 80% lines + branches | MUST |
| Util (FIFO) | ≥ 90% lines | MUST |
| Controller | happy path + error paths | MUST |
| Repository | CRUD + custom queries | KHUYẾN KHÍCH |

### Quy tắc

- Mỗi service class có 1 test class tương ứng
- Test cả happy path và error path
- Mock dependencies (repository, các service khác)
- Kiểm tra audit log được gọi đúng
- Kiểm tra transaction rollback khi exception
- Tên test method rõ ràng: `methodName_condition_expectedResult`

### Ví dụ Service Test

```java
class InventoryServiceTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private BatchRepository batchRepository;
    @InjectMocks private InventoryService inventoryService;

    @Test
    void reserveInventory_sufficientStock_shouldSucceed() {
        // Given
        Inventory inventory = Inventory.builder()
            .id(1L).totalQuantity(100).reservedQuantity(10).version(1)
            .build();
        when(inventoryRepository.findByWarehouseAndProduct(any(), any()))
            .thenReturn(Optional.of(inventory));

        // When
        inventoryService.reserveInventory(1L, 1L, 20);

        // Then
        assertThat(inventory.getReservedQuantity()).isEqualTo(30);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void reserveInventory_insufficientStock_shouldThrow() {
        // Given
        Inventory inventory = Inventory.builder()
            .id(1L).totalQuantity(100).reservedQuantity(95).version(1)
            .build();
        when(inventoryRepository.findByWarehouseAndProduct(any(), any()))
            .thenReturn(Optional.of(inventory));

        // When / Then
        assertThrows(InsufficientStockException.class,
            () -> inventoryService.reserveInventory(1L, 1L, 10));
        verify(inventoryRepository, never()).save(any());
    }
}
```

### Ví dụ FIFO Test

```java
class FifoSelectorTest {

    @Test
    void selectBatchesFifo_shouldPickOldestReceivedDate() {
        // Given
        List<Batch> batches = List.of(
            Batch.builder().id(1L).receivedDate(LocalDate.of(2026, 5, 15)).quantity(100).build(),
            Batch.builder().id(2L).receivedDate(LocalDate.of(2026, 5, 1)).quantity(50).build(),
            Batch.builder().id(3L).receivedDate(LocalDate.of(2026, 6, 1)).quantity(200).build()
        );

        // When
        List<Batch> result = FifoSelector.select(batches, 120);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(2L); // Nhập sớm nhất
        assertThat(result.get(1).getId()).isEqualTo(1L);
    }
}
```

## 2. Frontend — Jest + React Testing Library

### Cấu trúc

```
frontend/src/
├── components/**/*.test.jsx   # Component tests
├── hooks/*.test.js             # Hook tests
├── services/*.test.js          # API service tests
└── utils/*.test.js             # Utility tests
```

### Quy tắc

- Test business logic components (form validation, conditional rendering)
- Mock API calls (axios)
- Test user interactions (click, type, submit)
- Kiểm tra error states (loading, empty, error)

### Ví dụ Component Test

```javascript
import { render, screen, fireEvent } from '@testing-library/react';
import ReceiptTable from './ReceiptTable';

test('renders receipt list and handles click', () => {
    const mockReceipts = [
        { id: 1, code: 'NCC-001', status: 'PENDING_RECEIPT' },
    ];
    render(<ReceiptTable receipts={mockReceipts} onSelect={() => {}} />);
    expect(screen.getByText('NCC-001')).toBeInTheDocument();
});
```

## 3. Test Data Builders

Dùng Builder pattern để tạo test data:

```java
Inventory inventory = Inventory.builder()
    .id(1L).warehouseId(1L).productId(1L).batchId(1L)
    .totalQuantity(100).reservedQuantity(0).version(1)
    .build();
```
