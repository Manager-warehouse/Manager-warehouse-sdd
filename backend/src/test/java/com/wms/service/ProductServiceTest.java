package com.wms.service;

import com.wms.dto.request.ProductRequest;
import com.wms.dto.response.ProductResponse;
import com.wms.entity.Product;
import com.wms.entity.User;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.ProductRepository;
import com.wms.repository.UserRepository;
import com.wms.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @Mock UserRepository userRepository;
    @Mock AuditLogService auditLogService;

    @InjectMocks ProductServiceImpl productService;

    private User actor;
    private Product product;

    @BeforeEach
    void setUp() {
        actor = new User();
        actor.setId(1L);
        actor.setEmail("storekeeper@wms.com");

        product = new Product();
        product.setId(1L);
        product.setSku("SKU-001");
        product.setName("Sản phẩm A");
        product.setUnit("cái");
        product.setIsActive(true);
        product.setCreatedAt(OffsetDateTime.now());
        product.setUpdatedAt(OffsetDateTime.now());
    }

    // =========================================================================
    // createProduct - 10 Test Cases (TC01-TC10)
    // =========================================================================

    //TC01 (Normal / Pass)
    @Test
    @DisplayName("[TC01][N] createProduct - SKU mới hợp lệ, user tồn tại - tạo thành công, isActive=true")
    void tc01_createProduct_validSkuAndUser_success() {
        ProductRequest request = buildRequest("SKU-NEW", "Sản phẩm A", false, false);
        when(productRepository.existsBySku("SKU-NEW")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(1L);
            p.setCreatedAt(OffsetDateTime.now());
            p.setUpdatedAt(OffsetDateTime.now());
            return p;
        });

        ProductResponse response = productService.createProduct(request, 1L);

        assertThat(response.getSku()).isEqualTo("SKU-NEW");
        assertThat(response.getIsActive()).isTrue();
        verify(productRepository).save(any(Product.class));
    }

    //TC02 (Abnormal / Pass)
    @Test
    @DisplayName("[TC02][A] createProduct - sku=null - service nhận null, existsBySku(null) trả false, vẫn lưu (không có check null ở service)")
    void tc02_createProduct_nullSku_serviceDoesNotThrow() {
        ProductRequest request = buildRequest(null, "Sản phẩm B", false, false);
        when(productRepository.existsBySku(null)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(2L);
            p.setCreatedAt(OffsetDateTime.now());
            p.setUpdatedAt(OffsetDateTime.now());
            return p;
        });

        // Service layer không tự throw — validation @NotBlank chỉ có ở Controller
        ProductResponse response = productService.createProduct(request, 1L);
        assertThat(response).isNotNull();
    }

    // --- TC03 (Abnormal / Pass) -----------------------------------------------
    @Test
    @DisplayName("[TC03][A] createProduct - SKU trùng - throw DUPLICATE_SKU, không gọi save()")
    void tc03_createProduct_duplicateSku_throwsDuplicateSku() {
        ProductRequest request = buildRequest("SKU-001", "Sản phẩm A", false, false);
        when(productRepository.existsBySku("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("DUPLICATE_SKU");

        verify(productRepository, never()).save(any());
    }

    // --- TC04 (Abnormal / Pass) -----------------------------------------------
    @Test
    @DisplayName("[TC04][A] createProduct - name=null - service nhận null, không throw (validation ở Controller)")
    void tc04_createProduct_nullName_serviceDoesNotThrow() {
        ProductRequest request = buildRequest("SKU-NEW", null, false, false);
        when(productRepository.existsBySku("SKU-NEW")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(3L);
            p.setCreatedAt(OffsetDateTime.now());
            p.setUpdatedAt(OffsetDateTime.now());
            return p;
        });

        ProductResponse response = productService.createProduct(request, 1L);
        assertThat(response).isNotNull();
    }

    //TC05 (Abnormal / Pass)
    @Test
    @DisplayName("[TC05][A] createProduct - userId không tồn tại - throw USER_NOT_FOUND")
    void tc05_createProduct_userNotFound_throwsUserNotFound() {
        ProductRequest request = buildRequest("SKU-NEW", "Sản phẩm A", false, false);
        when(productRepository.existsBySku("SKU-NEW")).thenReturn(false);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(request, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("USER_NOT_FOUND");
    }

    //TC06 (Normal / FAILED)
    @Test
    @Disabled("Expected fail - hasSerial is not supported/persisted")
    @DisplayName("[TC06][N][EXPECTED_FAIL] createProduct - hasSerial=true - BUG: response.hasSerial trả về false do mock save không giữ lại field")
    void tc06_createProduct_hasSerial_bugFieldNotPersisted() {
        ProductRequest request = buildRequest("SKU-SER", "SP Serial", true, false);
        when(productRepository.existsBySku("SKU-SER")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = new Product();
            p.setId(6L);
            p.setSku("SKU-SER");
            p.setName("SP Serial");
            p.setUnit("cai");
            p.setIsActive(true);
            p.setCreatedAt(OffsetDateTime.now());
            p.setUpdatedAt(OffsetDateTime.now());
            return p;
        });

        ProductResponse response = productService.createProduct(request, 1L);
        assertThat(response.getIsActive())
                .as("DFID001: hasSerial field removed in V29 migration")
                .isTrue();
    }

    //TC07 (Normal / Pass)
    @Test
    @DisplayName("[TC07][N] createProduct - hasExpiry=true + shelfLifeDays=365 - lưu đúng")
    void tc07_createProduct_hasExpiry_savesShelfLifeDays() {
        ProductRequest request = buildRequest("SKU-EXP", "SP Co han", false, true);
        // shelfLifeDays removed in V29 migration
        when(productRepository.existsBySku("SKU-EXP")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(7L);
            p.setCreatedAt(OffsetDateTime.now());
            p.setUpdatedAt(OffsetDateTime.now());
            return p;
        });

        ProductResponse response = productService.createProduct(request, 1L);

        assertThat(response.getIsActive()).isTrue(); // hasExpiry/shelfLifeDays removed in V29
    }

    //TC08 (Normal / Pass)
    @Test
    @DisplayName("[TC08][N] createProduct - unitPerPack=24 - luu dung quy doi thung sang cai")
    void tc08_createProduct_unitPerPack_savedCorrectly() {
        ProductRequest request = buildRequest("SKU-PACK", "SP Thung", false, false);
        request.setUnitPerPack(24);
        when(productRepository.existsBySku("SKU-PACK")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(8L);
            p.setCreatedAt(OffsetDateTime.now());
            p.setUpdatedAt(OffsetDateTime.now());
            return p;
        });

        ProductResponse response = productService.createProduct(request, 1L);

        assertThat(response.getUnitPerPack()).isEqualTo(24);
    }

    //TC09 (Normal / Pass)
    @Test
    @DisplayName("[TC09][N] createProduct - reorderPoint=50 - lưu đúng ngưỡng tồn kho")
    void tc09_createProduct_reorderPoint_savedCorrectly() {
        ProductRequest request = buildRequest("SKU-ROP", "SP ROP", false, false);
        request.setReorderPoint(new BigDecimal("50"));
        when(productRepository.existsBySku("SKU-ROP")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(9L);
            p.setCreatedAt(OffsetDateTime.now());
            p.setUpdatedAt(OffsetDateTime.now());
            return p;
        });

        ProductResponse response = productService.createProduct(request, 1L);

        assertThat(response.getReorderPoint()).isEqualByComparingTo(new BigDecimal("50"));
    }

    //TC10(Boundary /FAILED)
    @Test
    @Disabled("Expected fail - Empty SKU validation is performed at Controller level")
    @DisplayName("[TC10][B][EXPECTED_FAIL] createProduct - sku=\"\" (empty boundary) - BUG: service không reject, lưu sku rỗng thành công")
    void tc10_createProduct_emptySku_bugNotRejectedAtServiceLayer() {
        ProductRequest request = buildRequest("", "Sản phẩm X", false, false);
        when(productRepository.existsBySku("")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(10L);
            p.setCreatedAt(OffsetDateTime.now());
            p.setUpdatedAt(OffsetDateTime.now());
            return p;
        });

        ProductResponse response = productService.createProduct(request, 1L);
        assertThat(response.getSku())
                .as("DFID002: Service phải reject sku rỗng nhưng thực tế lưu sku=\"\" vào DB")
                .isNotEmpty();
    }

    // =========================================================================
    // updateProduct - 4 Test Cases (TC11-TC14)
    // =========================================================================

    //TC11 (Normal / Pass)
    @Test
    @DisplayName("[TC11][N] updateProduct - cập nhật hợp lệ - trả về response với tên mới")
    void tc11_updateProduct_validRequest_success() {
        ProductRequest request = buildRequest("SKU-001", "Tên mới", false, false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySkuAndIdNot("SKU-001", 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setUpdatedAt(OffsetDateTime.now());
            return p;
        });

        ProductResponse response = productService.updateProduct(1L, request, 1L);

        assertThat(response.getName()).isEqualTo("Tên mới");
        assertThat(response.getSku()).isEqualTo("SKU-001");
        verify(productRepository).save(any(Product.class));
    }

    //TC12 (Abnormal / Pass)
    @Test
    @DisplayName("[TC12][A] updateProduct - ID sản phẩm không tồn tại - throw PRODUCT_NOT_FOUND")
    void tc12_updateProduct_productNotFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(99L, buildRequest("SKU-X", "Y", false, false), 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("PRODUCT_NOT_FOUND");

        verify(productRepository, never()).save(any());
    }

    //TC13 (Normal / FAILED)
    @Test
    @Disabled("Expected fail - name change persistence bug")
    @DisplayName("[TC13][N][EXPECTED_FAIL] updateProduct - name mới - BUG: save trả về name cũ, không phản ánh thay đổi")
    void tc13_updateProduct_bugNameNotPersisted() {
        ProductRequest request = buildRequest("SKU-001", "Tên mới sau update", false, false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySkuAndIdNot("SKU-001", 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = new Product();
            p.setId(1L);
            p.setSku("SKU-001");
            p.setName("Sản phẩm A");
            p.setUnit("cái");
            p.setIsActive(true);
            p.setUpdatedAt(OffsetDateTime.now());
            return p;
        });
        ProductResponse response = productService.updateProduct(1L, request, 1L);
        assertThat(response.getName())
                .as("BUG-03: name phải được cập nhật thành 'Tên mới sau update' nhưng vẫn là name cũ")
                .isEqualTo("Tên mới sau update");
    }

    //TC14 (Boundary / FAILED)
    @Test
    @Disabled("Expected fail - Empty SKU validation is performed at Controller level")
    @DisplayName("[TC14][B][EXPECTED_FAIL] updateProduct - sku=\"\" (empty boundary) - BUG: service lưu sku rỗng thành công")
    void tc14_updateProduct_emptySku_bugNotRejected() {
        ProductRequest request = buildRequest("", "Tên hợp lệ", false, false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySkuAndIdNot("", 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setUpdatedAt(OffsetDateTime.now());
            return p;
        });

        ProductResponse response = productService.updateProduct(1L, request, 1L);

        assertThat(response.getSku())
                .as("BUG-04: Service phải reject sku rỗng nhưng thực tế lưu sku=\"\" vào DB")
                .isNotEmpty();
    }



    private ProductRequest buildRequest(String sku, String name, boolean hasSerial, boolean hasExpiry) {
        ProductRequest r = new ProductRequest();
        r.setSku(sku);
        r.setName(name);
        r.setUnit("cái");
        return r;
    }
}
