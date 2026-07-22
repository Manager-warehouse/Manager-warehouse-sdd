package com.wms.service.billing_payment;


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
import com.wms.dto.request.AccountingPeriodCloseRequest;
import com.wms.dto.request.AccountingPeriodCreateRequest;
import com.wms.dto.response.AccountingPeriodResponse;
import com.wms.entity.billing_payment.AccountingPeriod;
import com.wms.entity.access_control.User;
import java.time.LocalDate;
import java.util.List;

public interface AccountingPeriodService {
    List<AccountingPeriodResponse> getAllPeriods(User actor);
    AccountingPeriodResponse createPeriod(AccountingPeriodCreateRequest request, User actor);
    AccountingPeriodResponse closePeriod(Long id, AccountingPeriodCloseRequest request, User actor);
    void validateDateInOpenPeriod(LocalDate date);

    // Validates the date falls in an OPEN period (auto-provisioning the next calendar
    // month if none exists yet for a future date) and returns that period, for entities
    // that need to stamp their accounting_period_id FK at creation time.
    AccountingPeriod resolveOpenPeriod(LocalDate date);
}
