package com.wms.repository.product_catalog;


import com.wms.entity.product_catalog.Product;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);

    Optional<Product> findBySkuAndIsActiveTrue(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    Optional<Product> findByIdAndIsActiveTrue(Long id);

    @Query("SELECT p FROM Product p ORDER BY p.name ASC")
    Page<Product> findAllProducts(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Product> findAllBySearch(@Param("search") String search, Pageable pageable);
}
