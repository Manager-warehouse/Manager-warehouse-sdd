package com.wms.service;

import com.wms.dto.response.BillingNotificationResponse;
import com.wms.entity.User;
import java.util.List;

public interface BillingNotificationService {
    List<BillingNotificationResponse> getActiveNotifications(User actor);
    void markAsRead(Long id, User actor);
}
