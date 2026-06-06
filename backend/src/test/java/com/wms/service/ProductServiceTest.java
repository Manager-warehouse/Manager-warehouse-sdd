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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @Mock UserRepository userRepository;

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
        product.setHasSerial(false);
        product.setHasExpiry(false);
        product.setIsActive(true);
        product.setCreatedAt(OffsetDateTime.now());
        product.setUpdatedAt(OffsetDateTime.now());
    }

    // -------------------------------------------------------------------------
    // getProducts
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getProducts - không có search - trả về page sản phẩm")
    void getProducts_noSearch_returnsPage() {
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findAllBySearch(null, PageRequest.of(0, 20))).thenReturn(page);

        Page<ProductResponse> result = productService.getProducts(null, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getSku()).isEqualTo("SKU-001");
    }

    @Test
    @DisplayName("getProducts - có search - trả về kết quả lọc")
    void getProducts_withSearch_returnsFiltered() {
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findAllBySearch(eq("SKU"), any())).thenReturn(page);

        Page<ProductResponse> result = productService.getProducts("SKU", PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    // -------------------------------------------------------------------------
    // getProduct
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getProduct - ID tồn tại - trả về ProductResponse")
    void getProduct_exists_returnsResponse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProduct(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getSku()).isEqualTo("SKU-001");
    }

    @Test
    @DisplayName("getProduct - ID không tồn tại - throw PRODUCT_NOT_FOUND")
    void getProduct_notFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("PRODUCT_NOT_FOUND");
    }

    // -------------------------------------------------------------------------
    // createProduct
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("createProduct - SKU hợp lệ - tạo thành công")
    void createProduct_validSku_createsProduct() {
        ProductRequest request = buildRequest("SKU-NEW", "Sản phẩm mới", false, false);
        when(productRepository.existsBySku("SKU-NEW")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(2L);
            p.setCreatedAt(OffsetDateTime.now());
            p.setUpdatedAt(OffsetDateTime.now());
            return p;
        });

        ProductResponse response = productService.createProduct(request, 1L);

        assertThat(response.getSku()).isEqualTo("SKU-NEW");
        assertThat(response.getIsActive()).isTrue();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("createProduct - SKU trùng - throw DUPLICATE_SKU")
    void createProduct_duplicateSku_throwsException() {
        ProductRequest request = buildRequest("SKU-001", "Tên", false, false);
        when(productRepository.existsBySku("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("DUPLICATE_SKU");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("createProduct - has_expiry=true - tạo thành công với shelf_life_days")
    void createProduct_hasExpiry_savesShelfLifeDays() {
        ProductRequest request = buildRequest("SKU-EXP", "SP Có hạn", false, true);
        request.setShelfLifeDays(365);

        when(productRepository.existsBySku("SKU-EXP")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(3L);
            p.setCreatedAt(OffsetDateTime.now());
            p.setUpdatedAt(OffsetDateTime.now());
            return p;
        });

        ProductResponse response = productService.createProduct(request, 1L);

        assertThat(response.getHasExpiry()).isTrue();
        assertThat(response.getShelfLifeDays()).isEqualTo(365);
    }

    @Test
    @DisplayName("createProduct - has_serial=true - tạo thành công")
    void createProduct_hasSerial_savedCorrectly() {
        ProductRequest request = buildRequest("SKU-SER", "SP Có serial", true, false);

        when(productRepository.existsBySku("SKU-SER")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(4L);
            p.setCreatedAt(OffsetDateTime.now());
            p.setUpdatedAt(OffsetDateTime.now());
            return p;
        });

        ProductResponse response = productService.createProduct(request, 1L);

        assertThat(response.getHasSerial()).isTrue();
    }

    @Test
    @DisplayName("createProduct - có unit_per_pack - lưu đúng thùng→cái")
    void createProduct_withUnitPerPack_savedCorrectly() {
        ProductRequest request = buildRequest("SKU-PACK", "SP Thùng", false, false);
        request.setUnitPerPack(24);

        when(productRepository.existsBySku("SKU-PACK")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(5L);
            p.setCreatedAt(OffsetDateTime.now());
            p.setUpdatedAt(OffsetDateTime.now());
            return p;
        });

        ProductResponse response = productService.createProduct(request, 1L);

        assertThat(response.getUnitPerPack()).isEqualTo(24);
    }

    // -------------------------------------------------------------------------
    // updateProduct
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("updateProduct - cập nhật hợp lệ - trả về response mới")
    void updateProduct_valid_updatesSuccessfully() {
        ProductRequest request = buildRequest("SKU-001-UPDATED", "Tên mới", false, false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySkuAndIdNot("SKU-001-UPDATED", 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(productRepository.save(any())).thenReturn(product);

        ProductResponse response = productService.updateProduct(1L, request, 1L);

        verify(productRepository).save(any(Product.class));
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("updateProduct - SKU trùng với sản phẩm khác - throw DUPLICATE_SKU")
    void updateProduct_duplicateSkuOtherProduct_throwsException() {
        ProductRequest request = buildRequest("SKU-002", "Tên", false, false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySkuAndIdNot("SKU-002", 1L)).thenReturn(true);

        assertThatThrownBy(() -> productService.updateProduct(1L, request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("DUPLICATE_SKU");
    }

    @Test
    @DisplayName("updateProduct - ID không tồn tại - throw PRODUCT_NOT_FOUND")
    void updateProduct_notFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(99L, buildRequest("X", "Y", false, false), 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("PRODUCT_NOT_FOUND");
    }

    // -------------------------------------------------------------------------
    // deactivateProduct
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("deactivateProduct - sản phẩm tồn tại - set is_active=false")
    void deactivateProduct_exists_setsInactive() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(productRepository.save(any())).thenReturn(product);

        productService.deactivateProduct(1L, 1L);

        assertThat(product.getIsActive()).isFalse();
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("deactivateProduct - ID không tồn tại - throw PRODUCT_NOT_FOUND")
    void deactivateProduct_notFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deactivateProduct(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("PRODUCT_NOT_FOUND");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ProductRequest buildRequest(String sku, String name, boolean hasSerial, boolean hasExpiry) {
        ProductRequest r = new ProductRequest();
        r.setSku(sku);
        r.setName(name);
        r.setUnit("cái");
        r.setHasSerial(hasSerial);
        r.setHasExpiry(hasExpiry);
        return r;
    }
}
