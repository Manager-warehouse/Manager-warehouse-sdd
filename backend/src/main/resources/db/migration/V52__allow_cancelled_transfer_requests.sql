-- Allow soft-delete semantics for transfer requests.
-- The application cancels draft/expired requests by setting status = 'CANCELLED'
-- instead of physically deleting rows, so the database constraint must allow it.
ALTER TABLE transfer_requests
    DROP CONSTRAINT IF EXISTS transfer_requests_status_check;

ALTER TABLE transfer_requests
    ADD CONSTRAINT transfer_requests_status_check
    CHECK (status IN (
        'DRAFT',
        'SUBMITTED',
        'APPROVED',
        'REJECTED',
        'CONVERTED',
        'CEO_APPROVED',
        'CEO_REJECTED',
        'CANCELLED'
    ));
