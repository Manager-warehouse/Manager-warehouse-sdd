package com.wms.service;

import com.wms.dto.request.AccountingPeriodCloseRequest;
import com.wms.dto.response.AccountingPeriodResponse;
import com.wms.entity.User;
import java.time.LocalDate;
import java.util.List;

public interface AccountingPeriodService {
    List<AccountingPeriodResponse> getAllPeriods(User actor);
    AccountingPeriodResponse closePeriod(Long id, AccountingPeriodCloseRequest request, User actor);
    void validateDateInOpenPeriod(LocalDate date);
}
