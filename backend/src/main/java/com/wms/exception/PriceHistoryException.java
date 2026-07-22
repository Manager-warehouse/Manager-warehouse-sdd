package com.wms.exception;


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
import org.springframework.http.HttpStatus;

/** Typed exception for pricing state/overlap violations. */
public class PriceHistoryException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public PriceHistoryException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }

    public static PriceHistoryException alreadyApproved() {
        return new PriceHistoryException(HttpStatus.CONFLICT, "PRICE_ALREADY_APPROVED",
                "Bản giá đã được duyệt, không thể thay đổi.");
    }

    public static PriceHistoryException alreadyCancelled() {
        return new PriceHistoryException(HttpStatus.CONFLICT, "PRICE_ALREADY_CANCELLED",
                "Bản giá đã bị hủy, không thể thay đổi.");
    }

    public static PriceHistoryException overlappingDate() {
        return new PriceHistoryException(HttpStatus.CONFLICT, "OVERLAPPING_EFFECTIVE_DATE",
                "Đã tồn tại bản giá PENDING hoặc APPROVED khác cùng effective_date cho sản phẩm/kho này.");
    }

    public static PriceHistoryException missingPrice(String productIds) {
        return new PriceHistoryException(HttpStatus.UNPROCESSABLE_ENTITY, "MISSING_PRICE",
                "Không tìm thấy bản giá APPROVED cho sản phẩm: " + productIds);
    }

    public static PriceHistoryException notFound(Long id) {
        return new PriceHistoryException(HttpStatus.NOT_FOUND, "PRICE_NOT_FOUND",
                "Bản giá không tồn tại: " + id);
    }

    public static PriceHistoryException sellingBelowCost() {
        return new PriceHistoryException(HttpStatus.UNPROCESSABLE_ENTITY, "SELLING_BELOW_COST",
                "selling_price phải lớn hơn cost_price.");
    }
}
