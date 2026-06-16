package com.wms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wms.enums.AccountingPeriodStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountingPeriodResponse {

    private Long id;

    @JsonProperty("period_name")
    private String periodName;

    @JsonProperty("start_date")
    private LocalDate startDate;

    @JsonProperty("end_date")
    private LocalDate endDate;

    private AccountingPeriodStatus status;

    @JsonProperty("closed_by")
    private Long closedById;

    @JsonProperty("closed_by_name")
    private String closedByName;

    @JsonProperty("closed_at")
    private OffsetDateTime closedAt;

    private String notes;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
}
