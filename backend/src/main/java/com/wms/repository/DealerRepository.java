package com.wms.repository;

import com.wms.entity.Dealer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DealerRepository extends JpaRepository<Dealer, Long> {
    boolean existsByCode(String code);
    Optional<Dealer> findByCode(String code);
}
