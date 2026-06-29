-- =============================================================================
-- Migration V2: Bổ sung hành động VIEW_REPORT vào constraint check của audit_logs
-- =============================================================================

ALTER TABLE public.audit_logs DROP CONSTRAINT IF EXISTS chk_audit_logs_action;
ALTER TABLE public.audit_logs DROP CONSTRAINT IF EXISTS audit_logs_action_check;

ALTER TABLE public.audit_logs ADD CONSTRAINT chk_audit_logs_action 
    CHECK (action IN ('LOGIN','LOGOUT','CREATE','UPDATE','STATUS_CHANGE','APPROVE','REJECT','CANCEL','SOFT_DELETE','ASSIGN','UNASSIGN','VIEW_REPORT'));
