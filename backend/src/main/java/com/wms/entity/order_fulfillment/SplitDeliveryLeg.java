package com.wms.entity.order_fulfillment;

import com.wms.entity.driver_management.Driver;
import com.wms.entity.fleet_management.Vehicle;
import com.wms.enums.order_fulfillment.SplitDeliveryPlanStatus;
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
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "split_delivery_legs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"split_plan_id", "stop_order"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SplitDeliveryLeg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "split_plan_id", nullable = false)
    private SplitDeliveryPlan splitPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SplitDeliveryPlanStatus status;

    @Column(name = "readiness_confirmed_at")
    private OffsetDateTime readinessConfirmedAt;

    @Column(name = "departed_at")
    private OffsetDateTime departedAt;

    @Column(name = "dealer_arrived_at")
    private OffsetDateTime dealerArrivedAt;

    @Column(name = "handover_confirmed_at")
    private OffsetDateTime handoverConfirmedAt;

    @Column(name = "failure_reported_at")
    private OffsetDateTime failureReportedAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
