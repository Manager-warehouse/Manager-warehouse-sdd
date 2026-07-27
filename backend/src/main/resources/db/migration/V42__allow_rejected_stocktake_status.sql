-- Spec 006 allows a warehouse manager to return a pending stocktake for recount.
ALTER TABLE stock_takes
    DROP CONSTRAINT IF EXISTS stock_takes_status_check;

ALTER TABLE stock_takes
    ADD CONSTRAINT stock_takes_status_check
    CHECK (status IN (
        'DRAFT',
        'IN_PROGRESS',
        'PENDING_APPROVAL',
        'APPROVED',
        'REJECTED',
        'CANCELLED'
    ));
