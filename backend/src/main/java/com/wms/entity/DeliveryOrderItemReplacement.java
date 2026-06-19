package com.wms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "delivery_order_item_replacements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryOrderItemReplacement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "do_item_id", nullable = false)
    private DeliveryOrderItem deliveryOrderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "failed_inventory_id", nullable = false)
    private Inventory failedInventory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replacement_inventory_id", nullable = false)
    private Inventory replacementInventory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "failed_batch_id", nullable = false)
    private Batch failedBatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "failed_location_id", nullable = false)
    private WarehouseLocation failedLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replacement_batch_id", nullable = false)
    private Batch replacementBatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replacement_location_id", nullable = false)
    private WarehouseLocation replacementLocation;

    @Column(name = "quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
