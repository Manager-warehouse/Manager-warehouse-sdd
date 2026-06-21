package com.wms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "inter_warehouse_transfer_allocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterWarehouseTransferAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_item_id", nullable = false)
    private InterWarehouseTransferItem transferItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @Column(name = "allocated_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal allocatedQty;
}
