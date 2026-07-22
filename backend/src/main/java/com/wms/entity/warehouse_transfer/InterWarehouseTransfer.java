package com.wms.entity.warehouse_transfer;


import com.wms.entity.access_control.*;
import com.wms.entity.audit_trail.*;
import com.wms.entity.billing_payment.*;
import com.wms.entity.dealer_management.*;
import com.wms.entity.document_numbering.*;
import com.wms.entity.driver_management.*;
import com.wms.entity.fleet_management.*;
import com.wms.entity.notification_delivery.*;
import com.wms.entity.order_fulfillment.*;
import com.wms.entity.price_management.*;
import com.wms.entity.product_catalog.*;
import com.wms.entity.stock_control.*;
import com.wms.entity.stock_counting.*;
import com.wms.entity.stock_receiving.*;
import com.wms.entity.supplier_management.*;
import com.wms.entity.user_configuration.*;
import com.wms.entity.warehouse_location.*;
import com.wms.entity.warehouse_transfer.*;
import com.wms.enums.access_control.*;
import com.wms.enums.audit_trail.*;
import com.wms.enums.billing_payment.*;
import com.wms.enums.dealer_management.*;
import com.wms.enums.driver_management.*;
import com.wms.enums.fleet_management.*;
import com.wms.enums.notification_delivery.*;
import com.wms.enums.order_fulfillment.*;
import com.wms.enums.price_management.*;
import com.wms.enums.stock_control.*;
import com.wms.enums.stock_counting.*;
import com.wms.enums.stock_receiving.*;
import com.wms.enums.supplier_management.*;
import com.wms.enums.user_configuration.*;
import com.wms.enums.warehouse_location.*;
import com.wms.enums.warehouse_transfer.*;
import lombok.*;
import com.wms.enums.warehouse_transfer.InterWarehouseTransferStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inter_warehouse_transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterWarehouseTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "transfer_number", nullable = false, unique = true, length = 50)
    private String transferNumber;

    @Column(name = "external_instruction_code", nullable = false, length = 100)
    private String externalInstructionCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_warehouse_id", nullable = false)
    private Warehouse sourceWarehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_warehouse_id", nullable = false)
    private Warehouse destinationWarehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private InterWarehouseTransferStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by")
    private User rejectedBy;

    @Column(name = "rejected_at")
    private OffsetDateTime rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @Column(name = "planned_date")
    private LocalDate plannedDate;

    @Column(name = "actual_received_date")
    private LocalDate actualReceivedDate;

    @Column(name = "discrepancy_reason", columnDefinition = "TEXT")
    private String discrepancyReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Column(name = "document_date", nullable = false)
    private LocalDate documentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accounting_period_id")
    private AccountingPeriod accountingPeriod;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "return_requested", nullable = false)
    @Builder.Default
    private boolean returnRequested = false;

    @Column(name = "return_reason", columnDefinition = "TEXT")
    private String returnReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_requested_by")
    private User returnRequestedBy;

    @Column(name = "return_requested_at")
    private OffsetDateTime returnRequestedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_approved_by")
    private User returnApprovedBy;

    @Column(name = "return_approved_at")
    private OffsetDateTime returnApprovedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_rejected_by")
    private User returnRejectedBy;

    @Column(name = "return_rejected_at")
    private OffsetDateTime returnRejectedAt;

    @Column(name = "return_rejection_reason", columnDefinition = "TEXT")
    private String returnRejectionReason;

    @Column(name = "is_returned", nullable = false)
    @Builder.Default
    private boolean isReturned = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_request_id")
    private TransferRequest transferRequest;

    // Outbound QC fields (T011)
    @Column(name = "outbound_qc_passed")
    private Boolean outboundQcPassed;

    @Column(name = "outbound_qc_note", columnDefinition = "TEXT")
    private String outboundQcNote;

    @Column(name = "outbound_qc_photo_ref")
    private String outboundQcPhotoRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outbound_qc_by")
    private User outboundQcBy;

    @Column(name = "outbound_qc_at")
    private OffsetDateTime outboundQcAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_loaded_reported_by")
    private User sourceLoadedReportedBy;

    @Column(name = "source_loaded_reported_at")
    private OffsetDateTime sourceLoadedReportedAt;

    @Column(name = "source_load_rework_required", nullable = false)
    @Builder.Default
    private boolean sourceLoadReworkRequired = false;

    @Column(name = "source_load_rework_reason", columnDefinition = "TEXT")
    private String sourceLoadReworkReason;

    // Load handover fields (T011)
    @Column(name = "load_handover_photo_ref")
    private String loadHandoverPhotoRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "load_handover_by")
    private User loadHandoverBy;

    @Column(name = "load_handover_at")
    private OffsetDateTime loadHandoverAt;

    // Arrival and handover at destination (T011)
    @Column(name = "driver_arrived_at")
    private OffsetDateTime driverArrivedAt;

    @Column(name = "arrival_handover_at")
    private OffsetDateTime arrivalHandoverAt;

    @Column(name = "arrival_handover_photo_ref")
    private String arrivalHandoverPhotoRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arrival_handover_by")
    private User arrivalHandoverBy;

    // Return leg fields (T011)
    @Column(name = "return_departed_at")
    private OffsetDateTime returnDepartedAt;

    @Column(name = "return_arrived_at")
    private OffsetDateTime returnArrivedAt;

    @Column(name = "return_arrival_handover_at")
    private OffsetDateTime returnArrivalHandoverAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_arrival_handover_by")
    private User returnArrivalHandoverBy;

    @Column(name = "return_photo_ref")
    private String returnPhotoRef;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "transfer", fetch = FetchType.LAZY)
    @Builder.Default
    private List<InterWarehouseTransferItem> items = new ArrayList<>();
}
