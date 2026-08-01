package com.wms.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.wms.enums.stock_receiving.StorekeeperReviewDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StorekeeperReviewRequest {

    @NotNull(message = "EXPECTED_VERSION_REQUIRED")
    @JsonAlias("expected_version")
    private Integer expectedVersion;

    @NotNull(message = "STOREKEEPER_REVIEW_DECISION_REQUIRED")
    private StorekeeperReviewDecision decision;

    @Size(max = 2000, message = "REASON_TOO_LONG")
    private String reason;
}
