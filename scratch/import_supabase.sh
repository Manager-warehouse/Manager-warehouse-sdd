#!/bin/bash
# Scratch script to import Supabase data into VPS database
set -e

echo "=== Starting Supabase database migration ==="

# Step 1: Dump data from Supabase
echo "1. Dumping data from Supabase..."
docker compose -f ~/wms/compose.prod.yaml exec -T db pg_dump -d "postgresql://postgres.jzniugklqehtghgzggiv:Warehouse12345se12@aws-1-ap-southeast-1.pooler.supabase.com:5432/postgres" --data-only --exclude-table=flyway_schema_history -f /tmp/supabase_data.sql

# Step 2: Create pre-import sql script to drop constraints and truncate tables
echo "2. Preparing database..."
docker compose -f ~/wms/compose.prod.yaml exec -T db sh -c 'cat << "EOF" > /tmp/clean_db_for_import.sql
-- Drop constraints temporarily to avoid import validation failures
ALTER TABLE public.delivery_orders DROP CONSTRAINT IF EXISTS delivery_orders_status_check;
ALTER TABLE public.inter_warehouse_transfers DROP CONSTRAINT IF EXISTS transfers_status_check;
ALTER TABLE public.audit_logs DROP CONSTRAINT IF EXISTS chk_audit_logs_action;
ALTER TABLE public.stock_take_items ALTER COLUMN actual_qty DROP NOT NULL;
ALTER TABLE public.inter_warehouse_transfer_items ALTER COLUMN batch_id DROP NOT NULL;

-- Truncate default seed data to prevent PKEY collisions
TRUNCATE TABLE public.users CASCADE;
TRUNCATE TABLE public.warehouses CASCADE;
TRUNCATE TABLE public.accounting_periods CASCADE;
TRUNCATE TABLE public.system_configs CASCADE;
EOF'

# Step 3: Run clean script on local Postgres
echo "3. Running clean script..."
docker compose -f ~/wms/compose.prod.yaml exec -T db psql -U wms_user -d wms -f /tmp/clean_db_for_import.sql

# Step 4: Rename tables and sequences in dump file
echo "4. Renaming old tables in dump file..."
docker compose -f ~/wms/compose.prod.yaml exec -T db sed -i 's/COPY public.transfers (/COPY public.inter_warehouse_transfers (/g' /tmp/supabase_data.sql
docker compose -f ~/wms/compose.prod.yaml exec -T db sed -i 's/COPY public.transfer_items (/COPY public.inter_warehouse_transfer_items (/g' /tmp/supabase_data.sql
docker compose -f ~/wms/compose.prod.yaml exec -T db sed -i 's/public.transfers_id_seq/public.inter_warehouse_transfers_id_seq/g' /tmp/supabase_data.sql
docker compose -f ~/wms/compose.prod.yaml exec -T db sed -i 's/public.transfer_items_id_seq/public.inter_warehouse_transfer_items_id_seq/g' /tmp/supabase_data.sql

# Step 5: Temporarily disable triggers/FK checks in dump file
echo "5. Prepending replica mode settings..."
docker compose -f ~/wms/compose.prod.yaml exec -T db sed -i "1i SET session_replication_role = 'replica';" /tmp/supabase_data.sql
docker compose -f ~/wms/compose.prod.yaml exec -T db sh -c "echo \"SET session_replication_role = 'origin';\" >> /tmp/supabase_data.sql"

# Step 6: Import SQL data
echo "6. Importing data..."
docker compose -f ~/wms/compose.prod.yaml exec -T db psql -U wms_user -d wms -f /tmp/supabase_data.sql

# Step 7: Fix enums/statuses of old data to match new enums in Java
echo "7. Mapping old status/action names to new ones..."
docker compose -f ~/wms/compose.prod.yaml exec -T db psql -U wms_user -d wms -c "
-- Delivery Orders: Map COMPLETED to DELIVERED
UPDATE public.delivery_orders SET status = 'DELIVERED' WHERE status = 'COMPLETED';
"

# Step 8: Re-apply constraints
echo "8. Re-applying check constraints..."
docker compose -f ~/wms/compose.prod.yaml exec -T db psql -U wms_user -d wms -c "
-- Re-add constraints
ALTER TABLE public.delivery_orders ADD CONSTRAINT delivery_orders_status_check CHECK (status::text = ANY (ARRAY['NEW'::character varying, 'PICKING'::character varying, 'READY_TO_SHIP'::character varying, 'IN_TRANSIT'::character varying, 'DELIVERED'::character varying, 'RETURNED'::character varying, 'CANCELLED'::character varying]::text[]));
ALTER TABLE public.inter_warehouse_transfers ADD CONSTRAINT transfers_status_check CHECK (status::text = ANY (ARRAY['NEW'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'IN_TRANSIT'::character varying, 'COMPLETED'::character varying, 'COMPLETED_WITH_DISCREPANCY'::character varying, 'CANCELLED'::character varying, 'QUARANTINED'::character varying]::text[]));
ALTER TABLE public.audit_logs ADD CONSTRAINT chk_audit_logs_action CHECK (action::text = ANY (ARRAY['LOGIN'::character varying, 'LOGOUT'::character varying, 'CREATE'::character varying, 'UPDATE'::character varying, 'STATUS_CHANGE'::character varying, 'APPROVE'::character varying, 'REJECT'::character varying, 'CANCEL'::character varying, 'SOFT_DELETE'::character varying, 'ASSIGN'::character varying, 'UNASSIGN'::character varying, 'UPLOAD_POD'::character varying, 'REQUEST_OTP'::character varying, 'CONFIRM_DELIVERY'::character varying, 'RESET_DELIVERY_OTP'::character varying, 'FAIL_DELIVERY'::character varying, 'TRIP_CREATE'::character varying, 'TRIP_UPDATE'::character varying, 'TRIP_CANCEL'::character varying, 'TRIP_DEPART'::character varying, 'DELIVERY_ATTEMPT_CREATE'::character varying, 'COMPLETE_TRIP'::character varying, 'PICKING_PLAN_SAVE'::character varying, 'PICKED_GOODS_RETURN_TO_BIN'::character varying, 'PICKING_REPLACEMENT_SAVE'::character varying, 'DELIVERY_ORDER_PICK_COMPLETE'::character varying, 'OUTBOUND_QC_FAIL_QUARANTINE'::character varying, 'DELIVERY_ORDER_QC_APPROVE'::character varying, 'DELIVERY_ORDER_WAREHOUSE_APPROVE'::character varying, 'DELIVERY_ORDER_WAREHOUSE_REJECT'::character varying, 'INVOICE_AUTO_CREATE'::character varying, 'RECEIPT_RETURN_CONFIRM'::character varying, 'RECEIPT_REJECT'::character varying, 'RECEIPT_APPROVE'::character varying, 'QUARANTINE_RTV_CREATE'::character varying, 'QUARANTINE_RTV_CONFIRM'::character varying, 'RECEIPT_QC_CONFIRM'::character varying, 'RECEIPT_QC_SUBMIT'::character varying, 'RECEIPT_PUTAWAY_COMPLETE'::character varying, 'INVENTORY_UPDATE'::character varying, 'TRANSFER_APPROVE'::character varying, 'TRANSFER_DEPART'::character varying, 'TRANSFER_UNSHIP'::character varying, 'TRANSFER_SHIP'::character varying, 'TRANSFER_TRIP_ASSIGN'::character varying, 'TRANSFER_DISCREPANCY_CREATE'::character varying, 'TRANSFER_RETURN_TO_SOURCE'::character varying, 'TRANSFER_QUARANTINE_REJECT'::character varying, 'TRANSFER_FINAL_RECEIVE'::character varying, 'TRANSFER_RECEIVE_CHECK'::character varying, 'TRANSFER_RECEIVE_COUNT'::character varying, 'TRANSFER_CANCEL'::character varying, 'TRANSFER_REJECT'::character varying, 'STOCKTAKE_CANCEL'::character varying, 'STOCKTAKE_REJECT'::character varying, 'STOCKTAKE_APPROVE'::character varying, 'STOCKTAKE_COMPLETE'::character varying, 'STOCKTAKE_AUTO_APPROVE'::character varying, 'STOCKTAKE_COUNT_UPDATE'::character varying, 'STOCKTAKE_START'::character varying, 'STOCKTAKE_CREATE'::character varying, 'PRICE_IMPORT'::character varying, 'PRICE_APPROVE'::character varying, 'PRICE_CANCEL'::character varying, 'PRICE_UPDATE'::character varying, 'PRICE_CREATE'::character varying, 'RECEIPT_CREATE'::character varying, 'CREDIT_NOTE_CREATE'::character varying, 'QUARANTINE_DISPOSAL_CREATE'::character varying, 'QUARANTINE_DISPOSAL_APPROVE'::character varying, 'TRANSFER_REQUEST_CREATE'::character varying, 'TRANSFER_REQUEST_UPDATE'::character varying, 'TRANSFER_REQUEST_SUBMIT'::character varying, 'TRANSFER_REQUEST_CEO_APPROVE'::character varying, 'TRANSFER_REQUEST_CEO_REJECT'::character varying, 'TRANSFER_REQUEST_CONVERT'::character varying, 'VIEW_REPORT'::character varying]::text[]));
"

# Step 9: Reset all serial sequences in public schema
echo "9. Resetting all database sequences..."
docker compose -f ~/wms/compose.prod.yaml exec -T db psql -U wms_user -d wms -c "
DO \$\$
DECLARE
    r RECORD;
BEGIN
    FOR r IN 
        SELECT 
            c.relname AS seq_name,
            t.relname AS table_name,
            a.attname AS col_name
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        JOIN pg_depend d ON d.objid = c.oid AND d.deptype = 'a'
        JOIN pg_class t ON t.oid = d.refobjid
        JOIN pg_attribute a ON a.attrelid = d.refobjid AND a.attnum = d.refobjsubid
        WHERE c.relkind = 'S' 
          AND n.nspname = 'public'
    LOOP
        EXECUTE format('SELECT setval(quote_ident(%L), COALESCE((SELECT max(%I) FROM %I), 1))', 
            r.seq_name, r.col_name, r.table_name);
    END LOOP;
END \$\$;
"

# Step 10: Clean up temp files
echo "10. Cleaning up..."
docker compose -f ~/wms/compose.prod.yaml exec -T db rm -f /tmp/supabase_data.sql /tmp/clean_db_for_import.sql

echo "=== Supabase database migration completed successfully! 🎉 ==="
