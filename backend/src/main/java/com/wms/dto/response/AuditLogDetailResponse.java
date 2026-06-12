package com.wms.dto;

import com.wms.entity.AuditLog;
import com.wms.util.AuditLogUtil;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Map;

@Getter
@Setter
public class AuditLogDetailResponse {
    private Long id;
    private OffsetDateTime timestamp;
    private Long actorId;
    private String actorName;
    private String actorRole;
    private String action;
    private String entityType;
    private Long entityId;
    private String description;
    private Long warehouseId;
    private String warehouseCode;
    private Map<String, Object> oldValue;
    private Map<String, Object> newValue;
    private String ipAddress;

    public static AuditLogDetailResponse from(AuditLog entity) {
        AuditLogDetailResponse dto = new AuditLogDetailResponse();
        dto.setId(entity.getId());
        dto.setTimestamp(entity.getTimestamp());
        if (entity.getActor() != null) {
            dto.setActorId(entity.getActor().getId());
            dto.setActorName(entity.getActor().getFullName());
        }
        dto.setActorRole(entity.getActorRole());
        dto.setAction(entity.getAction() == null ? null : entity.getAction().name());
        dto.setEntityType(entity.getEntityType());
        dto.setEntityId(entity.getEntityId());
        dto.setDescription(entity.getDescription());
        if (entity.getWarehouse() != null) {
            dto.setWarehouseId(entity.getWarehouse().getId());
            dto.setWarehouseCode(entity.getWarehouse().getCode());
        }
        dto.setOldValue(AuditLogUtil.fromJson(entity.getOldValue()));
        dto.setNewValue(AuditLogUtil.fromJson(entity.getNewValue()));
        dto.setIpAddress(entity.getIpAddress());
        return dto;
    }
}
