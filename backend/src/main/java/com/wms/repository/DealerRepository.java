package com.wms.repository;

import com.wms.entity.Dealer;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DealerRepository extends JpaRepository<Dealer, Long> {
    boolean existsByCode(String code);
    Optional<Dealer> findByCode(String code);

    // Serializes concurrent balance/credit-status mutations for the same dealer
    // (e.g. two deliveries invoicing the same dealer at once).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Dealer d where d.id = :id")
    Optional<Dealer> findByIdForUpdate(@Param("id") Long id);
}
