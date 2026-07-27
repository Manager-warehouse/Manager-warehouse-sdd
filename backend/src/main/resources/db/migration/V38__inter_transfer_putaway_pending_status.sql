-- Allow storekeeper putaway plans to wait for warehouse manager approval.
ALTER TABLE inter_warehouse_transfers
    DROP CONSTRAINT IF EXISTS inter_warehouse_transfers_status_check;

ALTER TABLE inter_warehouse_transfers
    ADD CONSTRAINT inter_warehouse_transfers_status_check
    CHECK (status IN (
        'NEW', 'APPROVED', 'REJECTED', 'IN_TRANSIT',
        'PUTAWAY_PENDING_APPROVAL',
        'COMPLETED', 'COMPLETED_WITH_DISCREPANCY',
        'CANCELLED', 'QUARANTINED', 'RETURNED'
    ));
