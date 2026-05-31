package com.wms.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "trip_delivery_orders",
        uniqueConstraints = @UniqueConstraint(columnNames = {"trip_id", "stop_order"}))
public class TripDeliveryOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "do_id", nullable = false, unique = true)
    private DeliveryOrder deliveryOrder;

    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder;
}
