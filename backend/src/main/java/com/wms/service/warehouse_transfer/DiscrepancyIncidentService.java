package com.wms.service.warehouse_transfer;

import com.wms.dto.request.DiscrepancyIncidentResolveRequest;
import com.wms.dto.response.DiscrepancyIncidentResponse;
import com.wms.entity.access_control.User;
import java.util.List;

public interface DiscrepancyIncidentService {
    List<DiscrepancyIncidentResponse> listIncidents(String status, User actor);

    DiscrepancyIncidentResponse resolveIncident(Long id, DiscrepancyIncidentResolveRequest request, User actor);
}
