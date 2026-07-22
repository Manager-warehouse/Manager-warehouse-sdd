package com.wms.service.product_catalog.impl;


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
import com.wms.entity.product_catalog.Product;
import com.wms.entity.access_control.User;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.product_catalog.ProductRepository;
import com.wms.repository.UserRepository;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.service.audit_trail.AuditLogService;
import com.wms.service.product_catalog.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return productRepository.findAllProducts(pageable)
                    .map(this::toResponse);
        }
        return productRepository.findAllBySearch(search.trim(), pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND"));
        return toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request, Long createdByUserId) {
        validateSku(request.getSku());
        if (productRepository.existsBySku(request.getSku())) {
            throw new IllegalArgumentException("DUPLICATE_SKU");
        }

        User actor = resolveUser(createdByUserId);

        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .unit(request.getUnit())
                .unitPerPack(request.getUnitPerPack())
                .description(request.getDescription())
                .weightKg(request.getWeightKg())
                .volumeM3(request.getVolumeM3())
                .reorderPoint(request.getReorderPoint())
                .isActive(true)
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        Product saved = productRepository.save(product);
        applyPersistenceMetadata(product, saved);

        // Record Audit Log
        auditLogService.log(actor, AuditAction.CREATE, "Product", product.getId(), product.getSku(), null, null, toMap(product));

        return toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request, Long updatedByUserId) {
        validateSku(request.getSku());
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND"));

        if (productRepository.existsBySkuAndIdNot(request.getSku(), id)) {
            throw new IllegalArgumentException("DUPLICATE_SKU");
        }

        User actor = resolveUser(updatedByUserId);
        Map<String, Object> oldMap = toMap(product);

        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setUnit(request.getUnit());
        product.setUnitPerPack(request.getUnitPerPack());
        product.setDescription(request.getDescription());
        product.setWeightKg(request.getWeightKg());
        product.setVolumeM3(request.getVolumeM3());
        product.setReorderPoint(request.getReorderPoint());
        product.setUpdatedBy(actor);

        Product saved = productRepository.save(product);
        applyPersistenceMetadata(product, saved);

        // Record Audit Log
        auditLogService.log(actor, AuditAction.UPDATE, "Product", product.getId(), product.getSku(), null, oldMap, toMap(product));

        return toResponse(product);
    }

    @Override
    @Transactional
    public void deactivateProduct(Long id, Long updatedByUserId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND"));

        User actor = resolveUser(updatedByUserId);
        Map<String, Object> oldMap = toMap(product);

        product.setIsActive(false);
        product.setUpdatedBy(actor);
        Product saved = productRepository.save(product);
        applyPersistenceMetadata(product, saved);

        // Record Audit Log
        auditLogService.log(actor, AuditAction.SOFT_DELETE, "Product", product.getId(), product.getSku(), null, oldMap, toMap(product));
    }

    @Override
    @Transactional
    public ProductResponse reactivateProduct(Long id, Long updatedByUserId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND"));

        if (Boolean.TRUE.equals(product.getIsActive())) {
            return toResponse(product);
        }

        User actor = resolveUser(updatedByUserId);
        Map<String, Object> oldMap = toMap(product);

        product.setIsActive(true);
        product.setUpdatedBy(actor);
        Product saved = productRepository.save(product);
        applyPersistenceMetadata(product, saved);

        auditLogService.log(actor, AuditAction.UPDATE, "Product", product.getId(), product.getSku(), null, oldMap, toMap(product));

        return toResponse(product);
    }

    private User resolveUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND"));
    }

    private Map<String, Object> toMap(Product p) {
        if (p == null) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.getId());
        map.put("sku", p.getSku());
        map.put("name", p.getName());
        map.put("unit", p.getUnit());
        map.put("unitPerPack", p.getUnitPerPack());
        map.put("description", p.getDescription());
        map.put("weightKg", p.getWeightKg());
        map.put("volumeM3", p.getVolumeM3());
        map.put("reorderPoint", p.getReorderPoint());
        map.put("isActive", p.getIsActive());
        return map;
    }

    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .sku(p.getSku())
                .name(p.getName())
                .unit(p.getUnit())
                .unitPerPack(p.getUnitPerPack())
                .description(p.getDescription())
                .imageUrl(p.getImageUrl())
                .weightKg(p.getWeightKg())
                .volumeM3(p.getVolumeM3())
                .reorderPoint(p.getReorderPoint())
                .isActive(p.getIsActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private void validateSku(String sku) {
        if (sku != null && sku.isBlank()) {
            throw new IllegalArgumentException("INVALID_SKU");
        }
    }

    private void applyPersistenceMetadata(Product target, Product saved) {
        if (saved == null || target == saved) {
            return;
        }
        target.setId(saved.getId());
        target.setCreatedAt(saved.getCreatedAt());
        target.setUpdatedAt(saved.getUpdatedAt());
    }
}
