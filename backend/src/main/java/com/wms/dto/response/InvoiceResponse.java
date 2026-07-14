package com.wms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wms.enums.InvoiceStatus;
import java.math.BigDecimal;
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
public class InvoiceResponse {

    private Long id;

    @JsonProperty("invoice_number")
    private String invoiceNumber;

    @JsonProperty("do_id")
    private Long doId;

    @JsonProperty("do_number")
    private String doNumber;

    @JsonProperty("dealer_id")
    private Long dealerId;

    @JsonProperty("dealer_name")
    private String dealerName;

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    @JsonProperty("issue_date")
    private LocalDate issueDate;

    @JsonProperty("due_date")
    private LocalDate dueDate;

    private InvoiceStatus status;

    @JsonProperty("created_by")
    private Long createdById;

    @JsonProperty("created_by_name")
    private String createdByName;

    @JsonProperty("document_date")
    private LocalDate documentDate;

    @JsonProperty("accounting_period_id")
    private Long accountingPeriodId;

    @JsonProperty("accounting_period_name")
    private String accountingPeriodName;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
}
