package com.wms.service.notification_delivery;


import com.wms.dto.response.BillingNotificationResponse;
import com.wms.entity.access_control.User;
import java.util.List;

public interface BillingNotificationService {
    List<BillingNotificationResponse> getActiveNotifications(User actor);
    void markAsRead(Long id, User actor);
}
