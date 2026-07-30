-- =============================================================================
-- V51: Sync chk_audit_logs_action constraint with AuditAction enum
-- =============================================================================
-- Tổng hợp toàn bộ AuditAction enum constants (AuditAction.java, 95 values).
-- Các migration trước (V18, V20, V32, V46) lần lượt thêm từng nhóm action
-- riêng lẻ. Migration này drop & tái tạo constraint một lần duy nhất với
-- đầy đủ danh sách hiện hành, tránh lỗi CHECK constraint khi insert.
--
-- Nguồn dữ liệu: com.wms.enums.audit_trail.AuditAction (142 dòng)
-- =============================================================================

ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS chk_audit_logs_action;

ALTER TABLE audit_logs ADD CONSTRAINT chk_audit_logs_action CHECK (action IN (

    -- -------------------------------------------------------------------------
    -- Generic / System
    -- -------------------------------------------------------------------------
    'LOGIN', 'LOGOUT',
    'CREATE', 'UPDATE', 'STATUS_CHANGE',
    'APPROVE', 'REJECT', 'CANCEL', 'SOFT_DELETE',
    'ASSIGN', 'UNASSIGN',
    'VIEW_REPORT',

    -- -------------------------------------------------------------------------
    -- Delivery / Trip / OTP
    -- -------------------------------------------------------------------------
    'UPLOAD_POD',
    'REQUEST_OTP', 'CONFIRM_DELIVERY', 'RESET_DELIVERY_OTP', 'FAIL_DELIVERY',
    'TRIP_CREATE', 'TRIP_UPDATE', 'TRIP_CANCEL', 'TRIP_DEPART',
    'DELIVERY_ATTEMPT_CREATE', 'COMPLETE_TRIP',

    -- -------------------------------------------------------------------------
    -- Outbound / Delivery Order
    -- -------------------------------------------------------------------------
    'PICKING_PLAN_SAVE', 'PICKED_GOODS_RETURN_TO_BIN',
    'PICKING_REPLACEMENT_SAVE', 'DELIVERY_ORDER_PICK_COMPLETE',
    'DELIVERY_ORDER_UPDATE',
    'OUTBOUND_QC_FAIL_QUARANTINE',
    'DELIVERY_ORDER_QC_APPROVE',
    'DELIVERY_ORDER_WAREHOUSE_APPROVE', 'DELIVERY_ORDER_WAREHOUSE_REJECT',

    -- -------------------------------------------------------------------------
    -- Return Flow (returned delivery QC rework)
    -- -------------------------------------------------------------------------
    'RETURN_FLOW_RECEIVE_CONFIRM',
    'RETURN_FLOW_COUNT_QC_SUBMIT',
    'RETURN_FLOW_QC_ACCEPT', 'RETURN_FLOW_QC_REJECT',
    'RETURN_FLOW_PUTAWAY_PLAN', 'RETURN_FLOW_PUTAWAY_COMPLETE',

    -- -------------------------------------------------------------------------
    -- Invoice / Billing
    -- -------------------------------------------------------------------------
    'INVOICE_AUTO_CREATE',
    'CREDIT_NOTE_CREATE',
    'CORRECTION_VOUCHER_CREATE',

    -- -------------------------------------------------------------------------
    -- Inbound / Receipt
    -- -------------------------------------------------------------------------
    'RECEIPT_CREATE',
    'RECEIPT_RECEIVE',
    'RECEIPT_PRE_RECEIVE_APPROVE', 'RECEIPT_PRE_RECEIVE_REJECT', 'RECEIPT_PRE_RECEIVE_RESUBMIT',
    'RECEIPT_QC_SUBMIT', 'RECEIPT_QC_CONFIRM',
    'RECEIPT_APPROVE', 'RECEIPT_PARTIAL_APPROVE', 'RECEIPT_REJECT',
    'RECEIPT_PUTAWAY_COMPLETE',
    'RECEIPT_RETURN_CONFIRM',
    'RECEIPT_CANCEL', 'RECEIPT_REOPEN', 'RECEIPT_CORRECTION',

    -- -------------------------------------------------------------------------
    -- Quarantine / RTV / Disposal
    -- -------------------------------------------------------------------------
    'QUARANTINE_RTV_CREATE', 'QUARANTINE_RTV_CONFIRM',
    'QUARANTINE_DISPOSAL_CREATE', 'QUARANTINE_DISPOSAL_APPROVE',

    -- -------------------------------------------------------------------------
    -- Inventory
    -- -------------------------------------------------------------------------
    'INVENTORY_UPDATE',

    -- -------------------------------------------------------------------------
    -- Inter-Warehouse Transfer
    -- -------------------------------------------------------------------------
    'TRANSFER_REQUEST_CREATE', 'TRANSFER_REQUEST_UPDATE',
    'TRANSFER_REQUEST_SUBMIT',
    'TRANSFER_REQUEST_CEO_APPROVE', 'TRANSFER_REQUEST_CEO_REJECT',
    'TRANSFER_REQUEST_CONVERT',
    'TRANSFER_APPROVE', 'TRANSFER_REJECT', 'TRANSFER_CANCEL',
    'TRANSFER_OUTBOUND_QC',
    'TRANSFER_SOURCE_LOAD_REPORT', 'TRANSFER_SOURCE_LOAD_REWORK',
    'TRANSFER_LOAD_HANDOVER',
    'TRANSFER_TRIP_ASSIGN',
    'TRANSFER_DEPART', 'TRANSFER_SHIP', 'TRANSFER_UNSHIP',
    'TRANSFER_ARRIVE', 'TRANSFER_ARRIVAL_HANDOVER',
    'TRANSFER_RECEIVE_CHECK', 'TRANSFER_RECEIVE_COUNT',
    'TRANSFER_FINAL_RECEIVE',
    'TRANSFER_DISCREPANCY_CREATE',
    'TRANSFER_RETURN_TO_SOURCE', 'TRANSFER_QUARANTINE_REJECT',
    'TRANSFER_RETURN_DEPART', 'TRANSFER_RETURN_ARRIVE', 'TRANSFER_RETURN_HANDOVER',

    -- -------------------------------------------------------------------------
    -- Stock Take
    -- -------------------------------------------------------------------------
    'STOCKTAKE_CREATE', 'STOCKTAKE_START',
    'STOCKTAKE_COUNT_UPDATE',
    'STOCKTAKE_COMPLETE',
    'STOCKTAKE_APPROVE', 'STOCKTAKE_AUTO_APPROVE',
    'STOCKTAKE_REJECT', 'STOCKTAKE_CANCEL',

    -- -------------------------------------------------------------------------
    -- Pricing
    -- -------------------------------------------------------------------------
    'PRICE_CREATE', 'PRICE_UPDATE', 'PRICE_IMPORT',
    'PRICE_APPROVE', 'PRICE_CANCEL'

));
