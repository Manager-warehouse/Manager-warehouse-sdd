package com.wms.service.billing_payment;

import com.wms.dto.response.SupplierBillingNotificationResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.stock_receiving.Receipt;
import java.util.List;

public interface SupplierBillingNotificationService {
    void createNotificationForReceiptOrder(Receipt receipt);
    List<SupplierBillingNotificationResponse> getPendingNotifications(User actor);
    void markAsRead(Long id, User actor);
}
