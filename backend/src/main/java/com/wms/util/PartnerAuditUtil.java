package com.wms.util;

import com.wms.entity.User;
import com.wms.enums.AuditAction;
import com.wms.service.AuditLogService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

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
