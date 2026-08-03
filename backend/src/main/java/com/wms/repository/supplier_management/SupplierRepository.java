package com.wms.repository.supplier_management;


import com.wms.entity.supplier_management.Supplier;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    boolean existsByCode(String code);
    Optional<Supplier> findByCode(String code);
}
