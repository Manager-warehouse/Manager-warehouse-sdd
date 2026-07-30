package com.wms.controller.warehouse_transfer;

import com.wms.dto.request.DiscrepancyIncidentResolveRequest;
import com.wms.dto.response.DiscrepancyIncidentResponse;
import com.wms.entity.access_control.User;
import com.wms.service.user_context.CurrentUserService;
import com.wms.service.warehouse_transfer.DiscrepancyIncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfer-discrepancy-incidents")
@Tag(name = "Transfer Discrepancy Incidents", description = "Shortage and over-receipt incident review after transfer receiving")
public class DiscrepancyIncidentController {

    private final DiscrepancyIncidentService incidentService;
    private final CurrentUserService currentUserService;

    public DiscrepancyIncidentController(DiscrepancyIncidentService incidentService,
                                         CurrentUserService currentUserService) {
        this.incidentService = incidentService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CEO','WAREHOUSE_MANAGER','ACCOUNTANT_MANAGER')")
    @Operation(summary = "List transfer discrepancy incidents")
    public List<DiscrepancyIncidentResponse> listIncidents(@RequestParam(required = false) String status) {
        return incidentService.listIncidents(status, currentUser());
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN','CEO','WAREHOUSE_MANAGER','ACCOUNTANT_MANAGER')")
    @Operation(summary = "Resolve an open transfer discrepancy incident")
    public DiscrepancyIncidentResponse resolveIncident(@PathVariable Long id,
                                                       @Valid @RequestBody DiscrepancyIncidentResolveRequest request) {
        return incidentService.resolveIncident(id, request, currentUser());
    }

    private User currentUser() {
        return currentUserService.getRequiredCurrentUser();
    }
}
