package com.wms.entity.billing_payment;


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
import com.wms.enums.billing_payment.BillingNotificationInvoiceStatus;
import com.wms.enums.billing_payment.BillingNotificationStatus;
import com.wms.enums.access_control.UserRole;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "billing_notifications",
        indexes = {
                @Index(name = "idx_billing_notifications_status_created",
                        columnList = "status, created_at"),
                @Index(name = "idx_billing_notifications_invoice_status",
                        columnList = "invoice_status"),
                @Index(name = "idx_billing_notifications_warehouse",
                        columnList = "warehouse_id"),
                @Index(name = "idx_billing_notifications_dealer",
                        columnList = "dealer_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "do_id", nullable = false)
    private DeliveryOrder deliveryOrder;

    @Column(name = "do_number", nullable = false, length = 50)
    private String doNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_id", nullable = false)
    private Dealer dealer;

    @Column(name = "dealer_name", nullable = false, length = 255)
    private String dealerName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "delivered_at", nullable = false)
    private OffsetDateTime deliveredAt;

    @Column(name = "total_amount_estimate", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmountEstimate;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_status", nullable = false, length = 30)
    private BillingNotificationInvoiceStatus invoiceStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BillingNotificationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_role", nullable = false, length = 50)
    private UserRole recipientRole;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        if (this.invoiceStatus == null) {
            this.invoiceStatus = BillingNotificationInvoiceStatus.NOT_INVOICED;
        }
        if (this.status == null) {
            this.status = BillingNotificationStatus.ACTIVE;
        }
        if (this.recipientRole == null) {
            this.recipientRole = UserRole.ACCOUNTANT;
        }
    }
}
