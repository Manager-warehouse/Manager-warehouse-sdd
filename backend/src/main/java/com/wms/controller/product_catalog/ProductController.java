package com.wms.controller.product_catalog;


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
import com.wms.dto.request.product_catalog.ProductRequest;
import com.wms.dto.response.product_catalog.ProductResponse;
import com.wms.entity.access_control.User;
import com.wms.repository.UserRepository;
import com.wms.service.product_catalog.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Quản lý danh mục sản phẩm (SKU)")
public class ProductController {

    private final ProductService productService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Danh sách sản phẩm", description = "Tìm kiếm theo SKU hoặc tên sản phẩm")
    public Page<ProductResponse> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productService.getProducts(search, PageRequest.of(page, size, Sort.by("name").ascending()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Chi tiết sản phẩm")
    public ProductResponse getProduct(@PathVariable Long id) {
        return productService.getProduct(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STOREKEEPER', 'WAREHOUSE_MANAGER', 'ADMIN')")
    @Operation(summary = "Tạo mới sản phẩm")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request,
            Authentication authentication) {
        Long userId = resolveUserId(authentication);
        ProductResponse response = productService.createProduct(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STOREKEEPER', 'WAREHOUSE_MANAGER', 'ADMIN')")
    @Operation(summary = "Cập nhật sản phẩm")
    public ProductResponse updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request,
            Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return productService.updateProduct(id, request, userId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STOREKEEPER', 'WAREHOUSE_MANAGER', 'ADMIN')")
    @Operation(summary = "Vô hiệu hóa sản phẩm (soft-delete)")
    public ResponseEntity<Void> deactivateProduct(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = resolveUserId(authentication);
        productService.deactivateProduct(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('STOREKEEPER', 'WAREHOUSE_MANAGER', 'ADMIN')")
    @Operation(summary = "Kích hoạt lại sản phẩm")
    public ProductResponse reactivateProduct(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return productService.reactivateProduct(id, userId);
    }

    private Long resolveUserId(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByEmail(username)
                .or(() -> userRepository.findByCode(username))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));
        return user.getId();
    }
}
