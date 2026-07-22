package com.wms.enums.audit_trail;


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
public enum AuditAction {
    LOGIN,
    LOGOUT,
    CREATE,
    UPDATE,
    STATUS_CHANGE,
    APPROVE,
    REJECT,
    CANCEL,
    SOFT_DELETE,
    ASSIGN,
    UNASSIGN,
    UPLOAD_POD,
    REQUEST_OTP,
    CONFIRM_DELIVERY,
    RESET_DELIVERY_OTP,
    FAIL_DELIVERY,
    TRIP_CREATE,
    TRIP_UPDATE,
    TRIP_CANCEL,
    TRIP_DEPART,
    DELIVERY_ATTEMPT_CREATE,
    COMPLETE_TRIP,
    PICKING_PLAN_SAVE,
    PICKED_GOODS_RETURN_TO_BIN,
    PICKING_REPLACEMENT_SAVE,
    DELIVERY_ORDER_PICK_COMPLETE,
    OUTBOUND_QC_FAIL_QUARANTINE,
    DELIVERY_ORDER_QC_APPROVE,
    DELIVERY_ORDER_WAREHOUSE_APPROVE,
    DELIVERY_ORDER_WAREHOUSE_REJECT,
    INVOICE_AUTO_CREATE,
    RECEIPT_RETURN_CONFIRM,
    RECEIPT_REJECT,
    RECEIPT_APPROVE,
    QUARANTINE_RTV_CREATE,
    QUARANTINE_RTV_CONFIRM,
    RECEIPT_QC_CONFIRM,
    RECEIPT_QC_SUBMIT,
    RECEIPT_PUTAWAY_COMPLETE,
    INVENTORY_UPDATE,
    TRANSFER_APPROVE,
    TRANSFER_DEPART,
    TRANSFER_UNSHIP,
    TRANSFER_SHIP,
    TRANSFER_TRIP_ASSIGN,
    TRANSFER_DISCREPANCY_CREATE,
    TRANSFER_RETURN_TO_SOURCE,
    TRANSFER_QUARANTINE_REJECT,
    TRANSFER_FINAL_RECEIVE,
    TRANSFER_RECEIVE_CHECK,
    TRANSFER_RECEIVE_COUNT,
    TRANSFER_CANCEL,
    TRANSFER_REJECT,
    STOCKTAKE_CANCEL,
    STOCKTAKE_REJECT,
    STOCKTAKE_APPROVE,
    STOCKTAKE_COMPLETE,
    STOCKTAKE_AUTO_APPROVE,
    STOCKTAKE_COUNT_UPDATE,
    STOCKTAKE_START,
    STOCKTAKE_CREATE,
    PRICE_IMPORT,
    PRICE_APPROVE,
    PRICE_CANCEL,
    PRICE_UPDATE,
    PRICE_CREATE,
    RECEIPT_CREATE,
    CREDIT_NOTE_CREATE,
    QUARANTINE_DISPOSAL_CREATE,
    QUARANTINE_DISPOSAL_APPROVE,
    VIEW_REPORT,
    TRANSFER_REQUEST_CREATE,
    TRANSFER_REQUEST_UPDATE,
    TRANSFER_REQUEST_SUBMIT,
    TRANSFER_REQUEST_CEO_APPROVE,
    TRANSFER_REQUEST_CEO_REJECT,
    TRANSFER_REQUEST_CONVERT,
    TRANSFER_OUTBOUND_QC,
    TRANSFER_LOAD_HANDOVER,
    TRANSFER_ARRIVE,
    TRANSFER_ARRIVAL_HANDOVER,
    TRANSFER_RETURN_DEPART,
    TRANSFER_RETURN_ARRIVE,
    TRANSFER_RETURN_HANDOVER
}

