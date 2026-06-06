package com.wms.service;

import com.wms.dto.request.ProductRequest;
import com.wms.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    Page<ProductResponse> getProducts(String search, Pageable pageable);

    ProductResponse getProduct(Long id);

    ProductResponse createProduct(ProductRequest request, Long createdByUserId);

    ProductResponse updateProduct(Long id, ProductRequest request, Long updatedByUserId);

    void deactivateProduct(Long id, Long updatedByUserId);
}
