package com.wms.service.notification_delivery.impl;


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
import com.wms.dto.response.BillingNotificationResponse;
import com.wms.entity.billing_payment.BillingNotification;
import com.wms.entity.order_fulfillment.Delivery;
import com.wms.entity.access_control.User;
import com.wms.enums.billing_payment.BillingNotificationInvoiceStatus;
import com.wms.enums.billing_payment.BillingNotificationStatus;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.BillingNotificationRepository;
import com.wms.repository.DeliveryRepository;
import com.wms.service.notification_delivery.BillingNotificationService;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingNotificationServiceImpl implements BillingNotificationService {

    private final BillingNotificationRepository billingNotificationRepository;
    private final DeliveryRepository deliveryRepository;

    public BillingNotificationServiceImpl(
            BillingNotificationRepository billingNotificationRepository,
            DeliveryRepository deliveryRepository) {
        this.billingNotificationRepository = billingNotificationRepository;
        this.deliveryRepository = deliveryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillingNotificationResponse> getActiveNotifications(User actor) {
        requireAccountantOrManager(actor);
        List<BillingNotification> notifications = billingNotificationRepository
                .findByInvoiceStatusOrderByCreatedAtDesc(BillingNotificationInvoiceStatus.NOT_INVOICED);
        
        return notifications.stream()
                .filter(n -> n.getStatus() == BillingNotificationStatus.ACTIVE)
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void markAsRead(Long id, User actor) {
        requireAccountantOrManager(actor);
        BillingNotification notification = billingNotificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Billing notification not found with id: " + id));
        
        if (notification.getStatus() == BillingNotificationStatus.ACTIVE) {
            notification.setStatus(BillingNotificationStatus.READ);
            notification.setReadAt(OffsetDateTime.now());
            billingNotificationRepository.save(notification);
        }
    }

    private void requireAccountantOrManager(User actor) {
        if (actor == null || (actor.getRole() != UserRole.ACCOUNTANT 
                && actor.getRole() != UserRole.ACCOUNTANT_MANAGER 
                && actor.getRole() != UserRole.ADMIN 
                && actor.getRole() != UserRole.CEO)) {
            throw new AccessDeniedException("Access denied: Accountant privileges required");
        }
    }

    private BillingNotificationResponse toResponse(BillingNotification entity) {
        // Tìm thông tin delivery để lấy OTP và các ảnh chụp POD
        Delivery delivery = deliveryRepository
                .findFirstByDeliveryOrderIdOrderByCreatedAtDesc(entity.getDeliveryOrder().getId())
                .orElse(null);

        OffsetDateTime otpVerifiedAt = null;
        String podImageUrl = null;
        String podSignatureUrl = null;
        OffsetDateTime podTimestamp = null;

        if (delivery != null) {
            otpVerifiedAt = delivery.getDeliveredAt(); // delivered_at ghi nhận thời điểm OTP được xác thực
            podImageUrl = delivery.getPodImageUrl();
            podSignatureUrl = delivery.getPodSignatureUrl();
            podTimestamp = delivery.getPodTimestamp();
        }

        return BillingNotificationResponse.builder()
                .id(entity.getId())
                .doId(entity.getDeliveryOrder().getId())
                .doNumber(entity.getDoNumber())
                .dealerId(entity.getDealer().getId())
                .dealerName(entity.getDealerName())
                .warehouseId(entity.getWarehouse().getId())
                .deliveredAt(entity.getDeliveredAt())
                .totalAmountEstimate(entity.getTotalAmountEstimate())
                .invoiceStatus(entity.getInvoiceStatus())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .readAt(entity.getReadAt())
                .otpVerifiedAt(otpVerifiedAt)
                .podImageUrl(podImageUrl)
                .podSignatureUrl(podSignatureUrl)
                .podTimestamp(podTimestamp)
                .build();
    }
}
