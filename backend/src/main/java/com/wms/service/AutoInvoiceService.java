package com.wms.service;

import com.wms.dto.outbound.AutoInvoiceResult;
import com.wms.entity.DeliveryOrder;
import com.wms.entity.User;

public interface AutoInvoiceService {

    AutoInvoiceResult createForConfirmedDelivery(DeliveryOrder deliveryOrder, User actor);
}
