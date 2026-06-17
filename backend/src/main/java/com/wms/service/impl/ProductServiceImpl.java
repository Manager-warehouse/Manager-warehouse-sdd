package com.wms.service.impl;

import com.wms.dto.request.ProductRequest;
import com.wms.dto.response.ProductResponse;
import com.wms.entity.Product;
import com.wms.entity.User;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.ProductRepository;
import com.wms.repository.UserRepository;
import com.wms.enums.AuditAction;
import com.wms.service.AuditLogService;
import com.wms.service.ProductService;
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
                .hasSerial(request.getHasSerial())
                .hasExpiry(request.getHasExpiry())
                .shelfLifeDays(request.getShelfLifeDays())
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
        product.setHasSerial(request.getHasSerial());
        product.setHasExpiry(request.getHasExpiry());
        product.setShelfLifeDays(request.getShelfLifeDays());
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
        map.put("hasSerial", p.getHasSerial());
        map.put("hasExpiry", p.getHasExpiry());
        map.put("shelfLifeDays", p.getShelfLifeDays());
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
                .hasSerial(p.getHasSerial())
                .hasExpiry(p.getHasExpiry())
                .shelfLifeDays(p.getShelfLifeDays())
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
