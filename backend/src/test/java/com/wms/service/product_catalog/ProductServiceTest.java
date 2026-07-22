package com.wms.service.product_catalog;


import com.wms.entity.access_control.*;
import com.wms.entity.audit_trail.*;
import com.wms.entity.billing_payment.*;
import com.wms.entity.dealer_management.*;
import com.wms.entity.document_numbering.*;
import com.wms.entity.driver_management.*;
import com.wms.entity.fleet_management.*;
import com.wms.entity.notification_delivery.*;
import com.wms.entity.order_fulfillment.*;
import com.wms.entity.price_management.*;
import com.wms.entity.product_catalog.*;
import com.wms.entity.stock_control.*;
import com.wms.entity.stock_counting.*;
import com.wms.entity.stock_receiving.*;
import com.wms.entity.supplier_management.*;
import com.wms.entity.user_configuration.*;
import com.wms.entity.warehouse_location.*;
import com.wms.entity.warehouse_transfer.*;
import com.wms.enums.access_control.*;
import com.wms.enums.audit_trail.*;
import com.wms.enums.billing_payment.*;
import com.wms.enums.dealer_management.*;
import com.wms.enums.driver_management.*;
import com.wms.enums.fleet_management.*;
import com.wms.enums.notification_delivery.*;
import com.wms.enums.order_fulfillment.*;
import com.wms.enums.price_management.*;
import com.wms.enums.stock_control.*;
import com.wms.enums.stock_counting.*;
import com.wms.enums.stock_receiving.*;
import com.wms.enums.supplier_management.*;
import com.wms.enums.user_configuration.*;
import com.wms.enums.warehouse_location.*;
import com.wms.enums.warehouse_transfer.*;
import com.wms.service.user_configuration.*;
import com.wms.service.user_configuration.impl.*;
import com.wms.service.audit_trail.*;
import com.wms.service.access_control.*;
import com.wms.service.dealer_management.*;
import com.wms.service.dealer_management.impl.*;
import com.wms.service.billing_payment.*;
import com.wms.service.billing_payment.impl.*;
import com.wms.service.stock_receiving.*;
import com.wms.service.stock_control.*;
import com.wms.service.stock_control.impl.*;
import com.wms.service.notification_delivery.*;
import com.wms.service.notification_delivery.impl.*;
import com.wms.service.order_fulfillment.*;
import com.wms.service.order_fulfillment.impl.*;
import com.wms.service.price_management.*;
import com.wms.service.price_management.impl.*;
import com.wms.service.reporting_alerting.*;
import com.wms.service.reporting_alerting.impl.*;
import com.wms.service.return_disposal.*;
import com.wms.service.stock_counting.*;
import com.wms.service.fleet_management.*;
import com.wms.service.fleet_management.impl.*;
import com.wms.service.warehouse_location.*;
import com.wms.service.warehouse_location.impl.*;

import com.wms.dto.request.product_catalog.ProductRequest;
import com.wms.dto.response.product_catalog.ProductResponse;
import com.wms.entity.product_catalog.Product;
import com.wms.entity.access_control.User;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.product_catalog.ProductRepository;
import com.wms.repository.UserRepository;
import com.wms.service.audit_trail.AuditLogService;
import com.wms.service.product_catalog.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

    @Mock
    ProductRepository productRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    AuditLogService auditLogService;

    @InjectMocks
    ProductServiceImpl productService;

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

    // TC01 (Normal / Pass)
    @Test
    @DisplayName("[TC01][N] createProduct - SKU mới hợp lệ, user tồn tại - tạo thành công, isActive=true")
    void tc01_createProduct_validSkuAndUser_success() {
        ProductRequest request = buildRequest("SKU-NEW", "Sản phẩm A");
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

    // TC02 (Abnormal / Pass)
    @Test
    @DisplayName("[TC02][A] createProduct - sku=null - service nhận null, existsBySku(null) trả false, vẫn lưu (không có check null ở service)")
    void tc02_createProduct_nullSku_serviceDoesNotThrow() {
        ProductRequest request = buildRequest(null, "Sản phẩm B");
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
        ProductRequest request = buildRequest("SKU-001", "Sản phẩm A");
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
        ProductRequest request = buildRequest("SKU-NEW", null);
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

    // TC05 (Abnormal / Pass)
    @Test
    @DisplayName("[TC05][A] createProduct - userId không tồn tại - throw USER_NOT_FOUND")
    void tc05_createProduct_userNotFound_throwsUserNotFound() {
        ProductRequest request = buildRequest("SKU-NEW", "Sản phẩm A");
        when(productRepository.existsBySku("SKU-NEW")).thenReturn(false);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(request, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("USER_NOT_FOUND");
    }

    // Expiry and serial tests removed.

    // TC08 (Normal / Pass)
    @Test
    @DisplayName("[TC08][N] createProduct - unitPerPack=24 - luu dung quy doi thung sang cai")
    void tc08_createProduct_unitPerPack_savedCorrectly() {
        ProductRequest request = buildRequest("SKU-PACK", "SP Thung");
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

    // TC09 (Normal / Pass)
    @Test
    @DisplayName("[TC09][N] createProduct - reorderPoint=50 - lưu đúng ngưỡng tồn kho")
    void tc09_createProduct_reorderPoint_savedCorrectly() {
        ProductRequest request = buildRequest("SKU-ROP", "SP ROP");
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

    // TC10(Boundary /FAILED)
    @Test
    @DisplayName("[TC10][B] createProduct - sku=\"\" (empty boundary) - service reject sku rỗng")
    void tc10_createProduct_emptySku_rejectedAtServiceLayer() {
        ProductRequest request = buildRequest("", "Sản phẩm X");

        assertThatThrownBy(() -> productService.createProduct(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INVALID_SKU");
        verify(productRepository, never()).save(any());
    }

    // =========================================================================
    // updateProduct - 4 Test Cases (TC11-TC14)
    // =========================================================================

    // TC11 (Normal / Pass)
    @Test
    @DisplayName("[TC11][N] updateProduct - cập nhật hợp lệ - trả về response với tên mới")
    void tc11_updateProduct_validRequest_success() {
        ProductRequest request = buildRequest("SKU-001", "Tên mới");
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

    // TC12 (Abnormal / Pass)
    @Test
    @DisplayName("[TC12][A] updateProduct - ID sản phẩm không tồn tại - throw PRODUCT_NOT_FOUND")
    void tc12_updateProduct_productNotFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(99L, buildRequest("SKU-X", "Y"), 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("PRODUCT_NOT_FOUND");

        verify(productRepository, never()).save(any());
    }

    // TC13 (Normal / FAILED)
    @Test
    @DisplayName("[TC13][N] updateProduct - name mới - trả về name mới sau update")
    void tc13_updateProduct_namePersisted() {
        ProductRequest request = buildRequest("SKU-001", "Tên mới sau update");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySkuAndIdNot("SKU-001", 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ProductResponse response = productService.updateProduct(1L, request, 1L);
        assertThat(response.getName())
                .isEqualTo("Tên mới sau update");
    }

    @Test
    @DisplayName("[TC14][B] updateProduct - sku=\"\" (empty boundary) - service reject sku rỗng")
    void tc14_updateProduct_emptySku_rejected() {
        ProductRequest request = buildRequest("", "Tên hợp lệ");

        assertThatThrownBy(() -> productService.updateProduct(1L, request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INVALID_SKU");
        verify(productRepository, never()).save(any());
    }

    private ProductRequest buildRequest(String sku, String name) {
        ProductRequest r = new ProductRequest();
        r.setSku(sku);
        r.setName(name);
        r.setUnit("cái");
        return r;
    }
}
