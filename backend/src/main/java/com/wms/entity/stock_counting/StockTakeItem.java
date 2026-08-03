package com.wms.entity.stock_counting;


import com.wms.entity.product_catalog.Product;
import com.wms.entity.stock_control.Batch;
import com.wms.entity.warehouse_location.WarehouseLocation;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity dòng hàng trong phiếu kiểm kê — bảng stock_take_items.
 *
 * Mỗi dòng = 1 tổ hợp (product + batch + location) trong kho.
 * system_qty: số lượng hệ thống tại thời điểm tạo/refresh.
 * actual_qty: số lượng thực tế đếm được (null khi chưa đếm).
 * variance_qty = actual_qty - system_qty, variance_value = variance_qty × cost_price.
 *
 * Dùng bởi: StockTakeService, StockTakeItemRepository
 */
@Entity
@Table(name = "stock_take_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTakeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_take_id", nullable = false)
    private StockTake stockTake;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private WarehouseLocation location;

    @Column(name = "system_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal systemQty;

    // Nullable until the storekeeper records the count; the create→start→count flow
    // inserts items before any actual count exists (Spec 006).
    @Column(name = "actual_qty", precision = 10, scale = 2)
    private BigDecimal actualQty;

    @Column(name = "variance_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal varianceQty;

    @Column(name = "variance_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal varianceValue;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
