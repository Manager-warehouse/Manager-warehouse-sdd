package com.wms.entity;

import lombok.*;
import jakarta.persistence.*;

@Entity
@Table(name = "trip_delivery_orders",
        uniqueConstraints = @UniqueConstraint(columnNames = {"trip_id", "stop_order"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripDeliveryOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "do_id", nullable = false)
    private DeliveryOrder deliveryOrder;

    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
