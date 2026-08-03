package com.wms.entity.stock_counting;


import com.wms.entity.access_control.User;
import com.wms.entity.billing_payment.AccountingPeriod;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.enums.order_fulfillment.ApprovalLevel;
import com.wms.enums.stock_counting.StockTakeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity phiếu kiểm kê hàng hóa — bảng stock_takes.
 *
 * Luồng trạng thái: DRAFT → IN_PROGRESS → PENDING_APPROVAL → APPROVED/REJECTED → CANCELLED
 * Quan hệ: warehouse (kho kiểm), conductedBy (thủ kho thực hiện), approvedBy (trưởng kho duyệt),
 *           accountingPeriod (kỳ kế toán), items (StockTakeItem — 1:N).
 *
 * Dùng bởi: StockTakeService, StockTakeRepository
 */
@Entity
@Table(name = "stock_takes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_take_number", nullable = false, unique = true, length = 50)
    private String stockTakeNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conducted_by", nullable = false)
    private User conductedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StockTakeStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_level", length = 10)
    private ApprovalLevel approvalLevel;

    @Builder.Default
    @Column(name = "is_employee_fault", nullable = false)
    private Boolean isEmployeeFault = false;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "total_variance_value", precision = 18, scale = 2)
    private BigDecimal totalVarianceValue;

    @Column(name = "stock_take_date", nullable = false)
    private LocalDate stockTakeDate;

    @Column(name = "document_date", nullable = false)
    private LocalDate documentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accounting_period_id")
    private AccountingPeriod accountingPeriod;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
