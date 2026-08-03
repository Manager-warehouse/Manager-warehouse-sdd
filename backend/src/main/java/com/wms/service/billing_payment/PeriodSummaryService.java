package com.wms.service.billing_payment;

import com.wms.dto.response.PeriodSummaryResponse;
import com.wms.entity.access_control.User;

public interface PeriodSummaryService {
    PeriodSummaryResponse getPeriodSummary(Long periodId, User actor);

    byte[] exportPeriodSummaryXlsx(Long periodId, User actor);
}
