package com.wms.repository;

import com.wms.entity.AccountingPeriod;
import com.wms.enums.AccountingPeriodStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, Long> {

    Optional<AccountingPeriod> findFirstByStatusOrderByStartDateDesc(AccountingPeriodStatus status);
}
