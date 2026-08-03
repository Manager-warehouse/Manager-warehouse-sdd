package com.wms.controller.order_fulfillment;

import com.wms.service.order_fulfillment.DriverDeliveryService;
import com.wms.service.order_fulfillment.PodEvidenceStorageService.StoredPodContent;
import com.wms.service.user_context.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/delivery-orders")
@Tag(name = "POD Evidence", description = "Authorized access to private proof-of-delivery evidence")
public class PodEvidenceController {

    private final DriverDeliveryService driverDeliveryService;
    private final CurrentUserService currentUserService;

    public PodEvidenceController(DriverDeliveryService driverDeliveryService,
            CurrentUserService currentUserService) {
        this.driverDeliveryService = driverDeliveryService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/{doId}/pod-evidence/{evidenceType}")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','ACCOUNTANT_MANAGER','PLANNER','STOREKEEPER',"
            + "'WAREHOUSE_STAFF','WAREHOUSE_MANAGER','DISPATCHER','ADMIN','CEO')")
    @Operation(summary = "Stream an authorized POD image from persistent local storage")
    public ResponseEntity<byte[]> getPodEvidence(@PathVariable Long doId,
            @PathVariable String evidenceType) {
        StoredPodContent content = driverDeliveryService.getPodEvidence(
                doId, evidenceType, currentUserService.getRequiredCurrentUser());
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(content.originalFilename())
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .contentLength(content.bytes().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(content.bytes());
    }
}
