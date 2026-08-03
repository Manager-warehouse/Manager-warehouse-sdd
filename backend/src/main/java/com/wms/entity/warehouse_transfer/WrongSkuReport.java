package com.wms.entity.warehouse_transfer;


import com.wms.entity.access_control.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "wrong_sku_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WrongSkuReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id", nullable = false)
    private InterWarehouseTransfer transfer;

    @Column(name = "status", nullable = false, length = 30)
    private String status; // 'PENDING', 'APPROVED', 'REJECTED'

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by", nullable = false)
    private User reportedBy;

    @Column(name = "reported_at", nullable = false)
    private OffsetDateTime reportedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_decision_by")
    private User managerDecisionBy;

    @Column(name = "manager_decision_at")
    private OffsetDateTime managerDecisionAt;

    @Column(name = "manager_note", columnDefinition = "TEXT")
    private String managerNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WrongSkuReportItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        reportedAt = OffsetDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
