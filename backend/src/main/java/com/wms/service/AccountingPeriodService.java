package com.wms.service;

import com.wms.dto.request.AccountingPeriodCloseRequest;
import com.wms.dto.request.AccountingPeriodCreateRequest;
import com.wms.dto.response.AccountingPeriodResponse;
import com.wms.entity.AccountingPeriod;
import com.wms.entity.User;
import java.time.LocalDate;
import java.util.List;

public interface AccountingPeriodService {
    List<AccountingPeriodResponse> getAllPeriods(User actor);
    AccountingPeriodResponse createPeriod(AccountingPeriodCreateRequest request, User actor);
    AccountingPeriodResponse closePeriod(Long id, AccountingPeriodCloseRequest request, User actor);
    void validateDateInOpenPeriod(LocalDate date);

    // Validates the date falls in an OPEN period (auto-provisioning the next calendar
    // month if none exists yet for a future date) and returns that period, for entities
    // that need to stamp their accounting_period_id FK at creation time.
    AccountingPeriod resolveOpenPeriod(LocalDate date);
}
