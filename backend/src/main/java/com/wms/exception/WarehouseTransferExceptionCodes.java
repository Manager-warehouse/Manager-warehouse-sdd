package com.wms.exception;

/**
 * Canonical error codes for the internal warehouse transfer flow.
 * Includes request/form validation codes and service-level business rule codes.
 */
public final class WarehouseTransferExceptionCodes {

    private WarehouseTransferExceptionCodes() {
    }

    public static final class Input {
        private Input() {
        }

        public static final String SOURCE_WAREHOUSE_ID_REQUIRED = "SOURCE_WAREHOUSE_ID_REQUIRED";
        public static final String DESTINATION_WAREHOUSE_ID_REQUIRED = "DESTINATION_WAREHOUSE_ID_REQUIRED";
        public static final String NEEDED_BY_DATE_REQUIRED = "NEEDED_BY_DATE_REQUIRED";
        public static final String NEEDED_BY_DATE_MUST_NOT_BE_PAST = "NEEDED_BY_DATE_MUST_NOT_BE_PAST";
        public static final String BUSINESS_REASON_REQUIRED = "BUSINESS_REASON_REQUIRED";
        public static final String ITEMS_REQUIRED = "ITEMS_REQUIRED";
        public static final String PRODUCT_ID_REQUIRED = "PRODUCT_ID_REQUIRED";
        public static final String REQUESTED_QTY_REQUIRED = "REQUESTED_QTY_REQUIRED";
        public static final String REQUESTED_QTY_MUST_BE_POSITIVE = "REQUESTED_QTY_MUST_BE_POSITIVE";
        public static final String EXTERNAL_INSTRUCTION_CODE_REQUIRED = "EXTERNAL_INSTRUCTION_CODE_REQUIRED";
        public static final String DOCUMENT_DATE_REQUIRED = "DOCUMENT_DATE_REQUIRED";
        public static final String PLANNED_DATE_REQUIRED = "PLANNED_DATE_REQUIRED";
        public static final String REASON_REQUIRED = "REASON_REQUIRED";
        public static final String PLANNED_QTY_REQUIRED = "PLANNED_QTY_REQUIRED";
        public static final String PLANNED_QTY_MUST_BE_POSITIVE = "PLANNED_QTY_MUST_BE_POSITIVE";
        public static final String TRANSFER_ITEM_ID_REQUIRED = "TRANSFER_ITEM_ID_REQUIRED";
        public static final String LOADED_QTY_REQUIRED = "LOADED_QTY_REQUIRED";
        public static final String INVALID_LOADED_QTY = "INVALID_LOADED_QTY";
        public static final String RECEIVED_QTY_REQUIRED = "RECEIVED_QTY_REQUIRED";
        public static final String RECEIVED_QTY_MUST_NOT_BE_NEGATIVE = "RECEIVED_QTY_MUST_NOT_BE_NEGATIVE";
        public static final String RECEIVE_COUNT_ITEMS_REQUIRED = "RECEIVE_COUNT_ITEMS_REQUIRED";
        public static final String CONFIRMED_QTY_REQUIRED = "CONFIRMED_QTY_REQUIRED";
        public static final String CONFIRMED_QTY_MUST_NOT_BE_NEGATIVE = "CONFIRMED_QTY_MUST_NOT_BE_NEGATIVE";
        public static final String RECEIVE_CHECK_ITEMS_REQUIRED = "RECEIVE_CHECK_ITEMS_REQUIRED";
        public static final String QC_PASSED_QTY_REQUIRED = "QC_PASSED_QTY_REQUIRED";
        public static final String QC_FAILED_QTY_REQUIRED = "QC_FAILED_QTY_REQUIRED";
        public static final String QC_QTY_MUST_NOT_BE_NEGATIVE = "QC_QTY_MUST_NOT_BE_NEGATIVE";
        public static final String QC_RESULT_REQUIRED = "QC_RESULT_REQUIRED";
        public static final String PHOTO_REF_REQUIRED = "PHOTO_REF_REQUIRED";
        public static final String VEHICLE_ID_REQUIRED = "VEHICLE_ID_REQUIRED";
        public static final String DRIVER_ID_REQUIRED = "DRIVER_ID_REQUIRED";
        public static final String PLANNED_START_AT_REQUIRED = "PLANNED_START_AT_REQUIRED";
        public static final String PLANNED_END_AT_REQUIRED = "PLANNED_END_AT_REQUIRED";
        public static final String REJECTION_REASON_REQUIRED = "REJECTION_REASON_REQUIRED";
        public static final String CANCEL_REASON_REQUIRED = "CANCEL_REASON_REQUIRED";
        public static final String RETURN_REASON_REQUIRED = "RETURN_REASON_REQUIRED";
        public static final String EXPECTED_PRODUCT_ID_REQUIRED = "EXPECTED_PRODUCT_ID_REQUIRED";
        public static final String ACTUAL_PRODUCT_ID_REQUIRED = "ACTUAL_PRODUCT_ID_REQUIRED";
        public static final String AFFECTED_QTY_REQUIRED = "AFFECTED_QTY_REQUIRED";
        public static final String WRONG_SKU_REASON_REQUIRED = "WRONG_SKU_REASON_REQUIRED";
        public static final String LOCATION_ID_REQUIRED = "LOCATION_ID_REQUIRED";
        public static final String PUTAWAY_ALLOCATIONS_REQUIRED = "PUTAWAY_ALLOCATIONS_REQUIRED";
        public static final String PUTAWAY_QUANTITY_REQUIRED = "PUTAWAY_QUANTITY_REQUIRED";
        public static final String PUTAWAY_QUANTITY_MUST_BE_POSITIVE = "PUTAWAY_QUANTITY_MUST_BE_POSITIVE";
        public static final String REASON_TOO_LONG = "REASON_TOO_LONG";
    }

    public static final class Access {
        private Access() {
        }

        public static final String WAREHOUSE_SCOPE_REQUIRED = "WAREHOUSE_SCOPE_REQUIRED";
        public static final String WAREHOUSE_MANAGER_ROLE_REQUIRED = "WAREHOUSE_MANAGER_ROLE_REQUIRED";
        public static final String CEO_ROLE_REQUIRED = "CEO_ROLE_REQUIRED";
        public static final String PLANNER_ROLE_REQUIRED = "PLANNER_ROLE_REQUIRED";
        public static final String ASSIGNED_DRIVER_REQUIRED = "ASSIGNED_DRIVER_REQUIRED";
        public static final String RETURN_HANDOVER_STOREKEEPER_REQUIRED = "RETURN_HANDOVER_STOREKEEPER_REQUIRED";
    }

    public static final class TransferRequestFlow {
        private TransferRequestFlow() {
        }

        public static final String SOURCE_DESTINATION_MUST_DIFFER = "SOURCE_DESTINATION_MUST_DIFFER";
        public static final String SOURCE_WAREHOUSE_MUST_BE_PHYSICAL = "SOURCE_WAREHOUSE_MUST_BE_PHYSICAL";
        public static final String DESTINATION_WAREHOUSE_MUST_BE_PHYSICAL = "DESTINATION_WAREHOUSE_MUST_BE_PHYSICAL";
        public static final String DUPLICATE_PRODUCT_IN_TRANSFER = "DUPLICATE_PRODUCT_IN_TRANSFER";
        public static final String TRANSFER_QTY_MUST_BE_WHOLE_NUMBER = "TRANSFER_QTY_MUST_BE_WHOLE_NUMBER";
        public static final String ONLY_DRAFT_CAN_BE_UPDATED = "ONLY_DRAFT_CAN_BE_UPDATED";
        public static final String ONLY_DRAFT_CAN_BE_CANCELLED = "ONLY_DRAFT_CAN_BE_CANCELLED";
        public static final String ONLY_DRAFT_CAN_BE_SUBMITTED = "ONLY_DRAFT_CAN_BE_SUBMITTED";
        public static final String ONLY_SUBMITTED_CAN_BE_APPROVED = "ONLY_SUBMITTED_CAN_BE_APPROVED";
        public static final String ONLY_SUBMITTED_CAN_BE_REJECTED = "ONLY_SUBMITTED_CAN_BE_REJECTED";
        public static final String ONLY_APPROVED_CAN_BE_CONVERTED = "ONLY_APPROVED_CAN_BE_CONVERTED";
        public static final String TRANSFER_REQUEST_QTY_EXCEEDS_SOURCE_AVAILABLE =
                "TRANSFER_REQUEST_QTY_EXCEEDS_SOURCE_AVAILABLE";
        public static final String TRANSFER_REQUEST_ALREADY_CONVERTED = "TRANSFER_REQUEST_ALREADY_CONVERTED";
    }

    public static final class TransferPlanning {
        private TransferPlanning() {
        }

        public static final String INVALID_TRANSFER_STATUS = "INVALID_TRANSFER_STATUS";
        public static final String DUPLICATE_EXTERNAL_INSTRUCTION = "DUPLICATE_EXTERNAL_INSTRUCTION";
        public static final String DOCUMENT_DATE_MUST_NOT_BE_PAST = "DOCUMENT_DATE_MUST_NOT_BE_PAST";
        public static final String PLANNED_DATE_MUST_NOT_BE_PAST = "PLANNED_DATE_MUST_NOT_BE_PAST";
        public static final String PLANNED_DATE_MUST_NOT_BE_BEFORE_DOCUMENT_DATE =
                "PLANNED_DATE_MUST_NOT_BE_BEFORE_DOCUMENT_DATE";
        public static final String INVALID_SOURCE_LOCATION = "INVALID_SOURCE_LOCATION";
        public static final String INVALID_DESTINATION_LOCATION = "INVALID_DESTINATION_LOCATION";
        public static final String TRANSFER_CANCEL_NOT_ALLOWED = "TRANSFER_CANCEL_NOT_ALLOWED";
        public static final String UNSHIP_REQUIRED_BEFORE_CANCEL = "UNSHIP_REQUIRED_BEFORE_CANCEL";
        public static final String INSUFFICIENT_AVAILABLE_STOCK = "INSUFFICIENT_AVAILABLE_STOCK";
    }

    public static final class Trip {
        private Trip() {
        }

        public static final String TRIP_ALREADY_DEPARTED = "TRIP_ALREADY_DEPARTED";
        public static final String TRIP_SCHEDULE_INVALID = "TRIP_SCHEDULE_INVALID";
        public static final String TRIP_START_IN_PAST = "TRIP_START_IN_PAST";
        public static final String TRIP_END_IN_PAST = "TRIP_END_IN_PAST";
        public static final String TRIP_CAPACITY_EXCEEDED = "TRIP_CAPACITY_EXCEEDED";
        public static final String VEHICLE_NOT_AVAILABLE = "VEHICLE_NOT_AVAILABLE";
        public static final String DRIVER_NOT_AVAILABLE = "DRIVER_NOT_AVAILABLE";
        public static final String VEHICLE_SCHEDULE_OVERLAP = "VEHICLE_SCHEDULE_OVERLAP";
        public static final String DRIVER_SCHEDULE_OVERLAP = "DRIVER_SCHEDULE_OVERLAP";
        public static final String VEHICLE_SOURCE_WAREHOUSE_REQUIRED = "VEHICLE_SOURCE_WAREHOUSE_REQUIRED";
        public static final String DRIVER_SOURCE_WAREHOUSE_REQUIRED = "DRIVER_SOURCE_WAREHOUSE_REQUIRED";
        public static final String TRANSFER_TRIP_REQUIRED = "TRANSFER_TRIP_REQUIRED";
    }

    public static final class SourceShipping {
        private SourceShipping() {
        }

        public static final String SOURCE_LOAD_ITEMS_REQUIRED = "SOURCE_LOAD_ITEMS_REQUIRED";
        public static final String TRANSFER_ITEM_NOT_FOUND = "TRANSFER_ITEM_NOT_FOUND";
        public static final String SOURCE_LOAD_REPORT_REQUIRED = "SOURCE_LOAD_REPORT_REQUIRED";
        public static final String SENT_QTY_MISMATCH = "SENT_QTY_MISMATCH";
        public static final String SOURCE_LOAD_REWORK_REQUIRED = "SOURCE_LOAD_REWORK_REQUIRED";
        public static final String SOURCE_LOAD_REWORK_REASON_REQUIRED = "SOURCE_LOAD_REWORK_REASON_REQUIRED";
        public static final String OUTBOUND_QC_REQUIRED = "OUTBOUND_QC_REQUIRED";
        public static final String OUTBOUND_QC_NOT_PASSED = "OUTBOUND_QC_NOT_PASSED";
        public static final String OUTBOUND_QC_FAILURE_REASON_REQUIRED = "OUTBOUND_QC_FAILURE_REASON_REQUIRED";
        public static final String LOAD_HANDOVER_REQUIRED = "LOAD_HANDOVER_REQUIRED";
        public static final String SENT_QTY_REQUIRED = "SENT_QTY_REQUIRED";
        public static final String IN_TRANSIT_WAREHOUSE_NOT_CONFIGURED = "IN_TRANSIT_WAREHOUSE_NOT_CONFIGURED";
        public static final String IN_TRANSIT_LOCATION_NOT_CONFIGURED = "IN_TRANSIT_LOCATION_NOT_CONFIGURED";
        public static final String INVENTORY_INVARIANT_VIOLATED = "INVENTORY_INVARIANT_VIOLATED";
    }

    public static final class DestinationReceiving {
        private DestinationReceiving() {
        }

        public static final String DRIVER_ARRIVE_REQUIRED = "DRIVER_ARRIVE_REQUIRED";
        public static final String ARRIVAL_HANDOVER_REQUIRED = "ARRIVAL_HANDOVER_REQUIRED";
        public static final String RETURN_REQUEST_PENDING = "RETURN_REQUEST_PENDING";
        public static final String ISSUE_REASON_REQUIRED = "ISSUE_REASON_REQUIRED";
        public static final String RECEIVE_QC_PHOTO_REQUIRED = "RECEIVE_QC_PHOTO_REQUIRED";
        public static final String WORKER_COUNT_REQUIRED = "WORKER_COUNT_REQUIRED";
        public static final String DUPLICATE_RECEIVE_COUNT_ITEM = "DUPLICATE_RECEIVE_COUNT_ITEM";
        public static final String DUPLICATE_RECEIVE_CHECK_ITEM = "DUPLICATE_RECEIVE_CHECK_ITEM";
        public static final String CHECKER_NOTE_REQUIRED = "CHECKER_NOTE_REQUIRED";
        public static final String QC_TOTAL_MUST_MATCH_CONFIRMED_QTY = "QC_TOTAL_MUST_MATCH_CONFIRMED_QTY";
        public static final String QC_FAILURE_REASON_REQUIRED = "QC_FAILURE_REASON_REQUIRED";
        public static final String QUARANTINE_LOCATION_NOT_CONFIGURED = "QUARANTINE_LOCATION_NOT_CONFIGURED";
        public static final String INVALID_DESTINATION_LOCATION = "INVALID_DESTINATION_LOCATION";
        public static final String QC_PASSED_BIN_MUST_NOT_BE_QUARANTINE = "QC_PASSED_BIN_MUST_NOT_BE_QUARANTINE";
        public static final String RECEIVE_CHECK_REQUIRED = "RECEIVE_CHECK_REQUIRED";
        public static final String IN_TRANSIT_STOCK_NOT_FOUND = "IN_TRANSIT_STOCK_NOT_FOUND";
    }

    public static final class Putaway {
        private Putaway() {
        }

        public static final String PUTAWAY_PLAN_REQUIRED = "PUTAWAY_PLAN_REQUIRED";
        public static final String PUTAWAY_PLAN_INVALID = "PUTAWAY_PLAN_INVALID";
        public static final String WAREHOUSE_MANAGER_APPROVAL_REQUIRED = "WAREHOUSE_MANAGER_APPROVAL_REQUIRED";
        public static final String DISCREPANCY_REASON_REQUIRED = "DISCREPANCY_REASON_REQUIRED";
        public static final String DUPLICATE_PUTAWAY_ITEM = "DUPLICATE_PUTAWAY_ITEM";
        public static final String DESTINATION_LOCATION_REQUIRED = "DESTINATION_LOCATION_REQUIRED";
        public static final String DUPLICATE_PUTAWAY_LOCATION = "DUPLICATE_PUTAWAY_LOCATION";
        public static final String PUTAWAY_QUANTITY_MUST_MATCH_QC_PASSED =
                "PUTAWAY_QUANTITY_MUST_MATCH_QC_PASSED";
        public static final String PUTAWAY_PLAN_EXHAUSTED = "PUTAWAY_PLAN_EXHAUSTED";
        public static final String BIN_CAPACITY_EXCEEDED = "BIN_CAPACITY_EXCEEDED";
    }

    public static final class ReturnFlow {
        private ReturnFlow() {
        }

        public static final String SOURCE_RETURN_ONLY_BEFORE_DESTINATION_ARRIVAL =
                "SOURCE_RETURN_ONLY_BEFORE_DESTINATION_ARRIVAL";
        public static final String RETURN_ALREADY_IN_PROGRESS = "RETURN_ALREADY_IN_PROGRESS";
        public static final String RETURN_REQUEST_ONLY_BEFORE_HANDOVER = "RETURN_REQUEST_ONLY_BEFORE_HANDOVER";
        public static final String RETURN_REQUEST_ONLY_BEFORE_COUNT = "RETURN_REQUEST_ONLY_BEFORE_COUNT";
        public static final String WRONG_SKU_ITEMS_REQUIRED = "WRONG_SKU_ITEMS_REQUIRED";
        public static final String NO_RETURN_REQUESTED = "NO_RETURN_REQUESTED";
        public static final String TRANSFER_NOT_RETURNED_LEG = "TRANSFER_NOT_RETURNED_LEG";
        public static final String RETURN_DEPART_REQUIRED = "RETURN_DEPART_REQUIRED";
        public static final String RETURN_ARRIVE_REQUIRED = "RETURN_ARRIVE_REQUIRED";
        public static final String RETURN_HANDOVER_REQUIRED = "RETURN_HANDOVER_REQUIRED";
        public static final String EXPECTED_PRODUCT_MISMATCH = "EXPECTED_PRODUCT_MISMATCH";
        public static final String ACTUAL_PRODUCT_MUST_DIFFER = "ACTUAL_PRODUCT_MUST_DIFFER";
        public static final String AFFECTED_QTY_MUST_BE_POSITIVE = "AFFECTED_QTY_MUST_BE_POSITIVE";
        public static final String AFFECTED_QTY_EXCEEDS_SENT_QTY = "AFFECTED_QTY_EXCEEDS_SENT_QTY";
    }

    public static final class PhotoEvidence {
        private PhotoEvidence() {
        }

        public static final String TRANSFER_PHOTO_FILE_INVALID = "TRANSFER_PHOTO_FILE_INVALID";
        public static final String TRANSFER_PHOTO_STORAGE_FAILED = "TRANSFER_PHOTO_STORAGE_FAILED";
    }
}
