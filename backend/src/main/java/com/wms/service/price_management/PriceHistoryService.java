package com.wms.service.price_management;


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
import com.wms.dto.request.PriceHistoryCreateRequest;
import com.wms.dto.request.PriceHistoryUpdateRequest;
import com.wms.dto.response.PriceHistoryResponse;
import com.wms.dto.response.PriceImportResponse;
import com.wms.dto.response.ProductPriceHistoryResponse;
import com.wms.entity.price_management.PriceHistory;
import com.wms.entity.access_control.User;
import com.wms.enums.price_management.PriceHistoryStatus;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PriceHistoryService {

    PriceHistoryResponse create(PriceHistoryCreateRequest request, User actor);

    PriceHistoryResponse update(Long id, PriceHistoryUpdateRequest request, User actor);

    PriceHistoryResponse cancel(Long id, User actor);

    PriceHistoryResponse approve(Long id, User actor);

    PriceHistoryResponse getById(Long id, User actor);

    List<PriceHistoryResponse> getAll(Long productId, Long warehouseId, PriceHistoryStatus status,
            LocalDate effectiveDateFrom, LocalDate effectiveDateTo);

    ProductPriceHistoryResponse getByProduct(Long productId);

    /** Price lookup for DO creation — scoped to the DO's warehouse. Returns empty if no APPROVED entry exists. */
    Optional<PriceHistory> lookupApproved(Long productId, Long warehouseId, LocalDate date);

    PriceImportResponse importFromExcel(MultipartFile file, User actor);
}
