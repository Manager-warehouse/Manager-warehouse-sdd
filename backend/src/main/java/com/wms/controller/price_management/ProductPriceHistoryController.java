package com.wms.controller.price_management;

import com.wms.dto.response.ProductPriceHistoryResponse;
import com.wms.service.price_management.PriceHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Price History", description = "Lịch sử giá sản phẩm")
public class ProductPriceHistoryController {

    private final PriceHistoryService priceHistoryService;

    @GetMapping("/{id}/price-history")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER', 'ADMIN', 'CEO')")
    @Operation(summary = "Lịch sử tất cả bản giá của một sản phẩm")
    public ProductPriceHistoryResponse getByProduct(@PathVariable Long id) {
        return priceHistoryService.getByProduct(id);
    }
}
