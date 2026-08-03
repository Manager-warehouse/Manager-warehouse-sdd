package com.wms.util;


import com.wms.entity.access_control.User;
import com.wms.entity.dealer_management.Dealer;
import com.wms.entity.supplier_management.Supplier;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.service.audit_trail.AuditLogService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Tiện ích ghi audit log cho đối tác (Dealer/Supplier) — tách riêng vì logic diff phức tạp hơn. */
@Component
public class PartnerAuditUtil {

    private final AuditLogService auditLogService;

    public PartnerAuditUtil(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    public void logChange(User actor,
                          AuditAction action,
                          String entityType,
                          Long entityId,
                          String entityCode,
                          Map<String, Object> before,
                          Map<String, Object> after) {
        auditLogService.log(actor, action, entityType, entityId, entityCode,
                null, before, after);
    }

    public static Map<String, Object> values(Object... pairs) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            values.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return values;
    }
}
