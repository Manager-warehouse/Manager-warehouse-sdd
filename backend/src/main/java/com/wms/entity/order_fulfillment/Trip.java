package com.wms.entity.order_fulfillment;


import com.wms.entity.access_control.User;
import com.wms.entity.driver_management.Driver;
import com.wms.entity.fleet_management.Vehicle;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.enums.order_fulfillment.TripStatus;
import com.wms.enums.order_fulfillment.TripType;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_number", nullable = false, unique = true, length = 50)
    private String tripNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatcher_id", nullable = false)
    private User dispatcher;

    @Column(name = "planned_date", nullable = false)
    private LocalDate plannedDate;

    @Column(name = "planned_start_at", nullable = false)
    private LocalDateTime plannedStartAt;

    @Column(name = "planned_end_at", nullable = false)
    private LocalDateTime plannedEndAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_type", nullable = false, length = 20)
    private TripType tripType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TripStatus status;

    @Column(name = "total_weight_kg", precision = 10, scale = 2)
    private BigDecimal totalWeightKg;

    @Column(name = "total_volume_m3", precision = 10, scale = 3)
    private BigDecimal totalVolumeM3;

    @Column(name = "calculated_weight_kg", precision = 10, scale = 2)
    private BigDecimal calculatedWeightKg;

    @Column(name = "calculated_volume_m3", precision = 10, scale = 4)
    private BigDecimal calculatedVolumeM3;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "departed_at")
    private OffsetDateTime departedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
