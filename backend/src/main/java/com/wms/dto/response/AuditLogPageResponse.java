package com.wms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogPageResponse {
    private List<AuditLogListItemResponse> data;
    private int page;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
    private boolean requiresFilterForOlder;
}
