package com.wms.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record WrongSkuReportResponse(
    Long id,
    Long transferId,
    String status,
    Long reportedById,
    String reportedByName,
    OffsetDateTime reportedAt,
    Long managerDecisionById,
    String managerDecisionByName,
    OffsetDateTime managerDecisionAt,
    String managerNote,
    List<WrongSkuReportItemResponse> items
) {}
