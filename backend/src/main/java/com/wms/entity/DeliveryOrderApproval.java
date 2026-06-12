package com.wms.entity;

import lombok.*;
import com.wms.enums.ApprovalResult;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "delivery_order_approvals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryOrderApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "do_id", nullable = false)
    private DeliveryOrder deliveryOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", nullable = false)
    private User approver;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    private ApprovalResult result;

    @Column(name = "contract_image_url", length = 500)
    private String contractImageUrl;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "approved_at", nullable = false)
    private OffsetDateTime approvedAt;
}
