package com.wms.service.product_catalog;


import com.wms.dto.request.product_catalog.ProductRequest;
import com.wms.dto.response.product_catalog.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    Page<ProductResponse> getProducts(String search, Pageable pageable);

    ProductResponse getProduct(Long id);

    ProductResponse createProduct(ProductRequest request, Long createdByUserId);

    ProductResponse updateProduct(Long id, ProductRequest request, Long updatedByUserId);

    void deactivateProduct(Long id, Long updatedByUserId);

    ProductResponse reactivateProduct(Long id, Long updatedByUserId);
}
