package com.wms.entity.order_fulfillment;

import com.wms.entity.access_control.User;
import com.wms.enums.order_fulfillment.ReturnedDeliveryFlowStatus;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "returned_delivery_flows")
@Getter
@Setter
public class ReturnedDeliveryFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "do_id", nullable = false, unique = true)
    private DeliveryOrder deliveryOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReturnedDeliveryFlowStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counted_by_staff_id")
    private User countedByStaff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_storekeeper_id")
    private User approvedByStorekeeper;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "putaway_planned_by_storekeeper_id")
    private User putawayPlannedByStorekeeper;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "putaway_completed_by_staff_id")
    private User putawayCompletedByStaff;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;

    @Column(name = "putaway_notes", columnDefinition = "TEXT")
    private String putawayNotes;

    @Column(name = "completion_notes", columnDefinition = "TEXT")
    private String completionNotes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @OneToMany(mappedBy = "flow", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<ReturnedDeliveryFlowItem> items = new ArrayList<>();
}
