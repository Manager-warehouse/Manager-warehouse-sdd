package com.wms.repository;

import com.wms.entity.AccountingPeriod;
import com.wms.enums.AccountingPeriodStatus;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, Long> {

    @Query("SELECT ap FROM AccountingPeriod ap WHERE ap.status = :status " +
           "AND ap.startDate <= :date AND ap.endDate >= :date")
    Optional<AccountingPeriod> findOpenPeriodForDate(
            @Param("status") AccountingPeriodStatus status,
            @Param("date") LocalDate date);
}
