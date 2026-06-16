package com.wms.repository;

import com.wms.entity.AccountingPeriod;
import com.wms.enums.AccountingPeriodStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, Long> {

    Optional<AccountingPeriod> findByPeriodName(String periodName);

    List<AccountingPeriod> findAllByOrderByStartDateDesc();

    @Query("select a from AccountingPeriod a where :date >= a.startDate and :date <= a.endDate")
    Optional<AccountingPeriod> findPeriodByDate(@Param("date") LocalDate date);

    @Query("select a from AccountingPeriod a where :date >= a.startDate and :date <= a.endDate and a.status = :status")
    Optional<AccountingPeriod> findPeriodByDateAndStatus(
            @Param("date") LocalDate date,
            @Param("status") AccountingPeriodStatus status);
}
