package com.wms.dto;

import com.wms.entity.AuditLog;
import com.wms.util.AuditLogUtil;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Map;

@Getter
@Setter
public class AuditLogResponse {
    private Long id;
    private Long actorId;
    private String actorName;
    private String actorRole;
    private String action;
    private String entityType;
    private Long entityId;
    private String description;
    private Long warehouseId;
    private Map<String, Object> oldValue;
    private Map<String, Object> newValue;
    private OffsetDateTime timestamp;
    private String ipAddress;

    public static AuditLogResponse from(AuditLog entity) {
        if (entity == null) {
            return null;
        }
        AuditLogResponse dto = new AuditLogResponse();
        dto.setId(entity.getId());
        if (entity.getActor() != null) {
            dto.setActorId(entity.getActor().getId());
            dto.setActorName(entity.getActor().getFullName());
        }
        dto.setActorRole(entity.getActorRole());
        if (entity.getAction() != null) {
            dto.setAction(entity.getAction().name());
        }
        dto.setEntityType(entity.getEntityType());
        dto.setEntityId(entity.getEntityId());
        dto.setDescription(entity.getDescription());
        if (entity.getWarehouse() != null) {
            dto.setWarehouseId(entity.getWarehouse().getId());
        }
        dto.setOldValue(AuditLogUtil.fromJson(entity.getOldValue()));
        dto.setNewValue(AuditLogUtil.fromJson(entity.getNewValue()));
        dto.setTimestamp(entity.getTimestamp());
        dto.setIpAddress(entity.getIpAddress());
        return dto;
    }
}
