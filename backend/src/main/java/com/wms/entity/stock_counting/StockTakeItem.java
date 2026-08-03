package com.wms.entity.stock_counting;


import com.wms.entity.access_control.*;
import com.wms.entity.audit_trail.*;
import com.wms.entity.billing_payment.*;
import com.wms.entity.dealer_management.*;
import com.wms.entity.document_numbering.*;
import com.wms.entity.driver_management.*;
import com.wms.entity.fleet_management.*;
import com.wms.entity.notification_delivery.*;
import com.wms.entity.order_fulfillment.*;
import com.wms.entity.price_management.*;
import com.wms.entity.product_catalog.*;
import com.wms.entity.stock_control.*;
import com.wms.entity.stock_counting.*;
import com.wms.entity.stock_receiving.*;
import com.wms.entity.supplier_management.*;
import com.wms.entity.user_configuration.*;
import com.wms.entity.warehouse_location.*;
import com.wms.entity.warehouse_transfer.*;
import com.wms.enums.access_control.*;
import com.wms.enums.audit_trail.*;
import com.wms.enums.billing_payment.*;
import com.wms.enums.dealer_management.*;
import com.wms.enums.driver_management.*;
import com.wms.enums.fleet_management.*;
import com.wms.enums.notification_delivery.*;
import com.wms.enums.order_fulfillment.*;
import com.wms.enums.price_management.*;
import com.wms.enums.stock_control.*;
import com.wms.enums.stock_counting.*;
import com.wms.enums.stock_receiving.*;
import com.wms.enums.supplier_management.*;
import com.wms.enums.user_configuration.*;
import com.wms.enums.warehouse_location.*;
import com.wms.enums.warehouse_transfer.*;
import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;

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
