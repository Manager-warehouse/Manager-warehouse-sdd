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
    private long totalItems;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
    private boolean requiresFilterForOlder;

    public AuditLogPageResponse(List<AuditLogListItemResponse> data,
                                int page,
                                int pageSize,
                                boolean hasNext,
                                boolean hasPrevious,
                                boolean requiresFilterForOlder) {
        this(data, page, pageSize, data == null ? 0 : data.size(), 1,
                hasNext, hasPrevious, requiresFilterForOlder);
    }
}
