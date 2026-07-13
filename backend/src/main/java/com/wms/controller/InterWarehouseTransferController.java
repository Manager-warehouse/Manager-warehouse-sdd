package com.wms.controller;

import com.wms.dto.request.*;
import com.wms.dto.response.InterWarehouseTransferResponse;
import com.wms.dto.response.TransferPhotoUploadResponse;
import com.wms.entity.User;
import com.wms.service.CurrentUserService;
import com.wms.service.transfer.InterWarehouseTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/inter-warehouse-transfers")
@Tag(name = "Inter-Warehouse Transfers", description = "Spec 005 inter-warehouse transfer planning, shipping, and receiving")
public class InterWarehouseTransferController {

    private final InterWarehouseTransferService transferService;
    private final CurrentUserService currentUserService;

    public InterWarehouseTransferController(InterWarehouseTransferService transferService,
                                            CurrentUserService currentUserService) {
        this.transferService = transferService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CEO','PLANNER','DISPATCHER','WAREHOUSE_MANAGER','STOREKEEPER','WAREHOUSE_STAFF','DRIVER')")
    @Operation(summary = "List inter-warehouse transfers")
    public List<InterWarehouseTransferResponse> getAllTransfers() {
        return transferService.getAllTransfers(currentUser());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CEO','PLANNER','DISPATCHER','WAREHOUSE_MANAGER','STOREKEEPER','WAREHOUSE_STAFF','DRIVER')")
    @Operation(summary = "Get inter-warehouse transfer detail")
    public InterWarehouseTransferResponse getTransferById(@PathVariable Long id) {
        return transferService.getTransferById(id, currentUser());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Create NEW transfer from external instruction")
    public InterWarehouseTransferResponse createTransfer(@Valid @RequestBody InterWarehouseTransferCreateRequest request) {
        return transferService.createTransfer(request, currentUser());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Replace NEW transfer details and item list")
    public InterWarehouseTransferResponse updateTransfer(@PathVariable Long id,
                                                         @Valid @RequestBody InterWarehouseTransferUpdateRequest request) {
        return transferService.updateTransfer(id, request, currentUser());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PLANNER','WAREHOUSE_MANAGER')")
    @Operation(summary = "Cancel NEW or unshipped APPROVED transfer")
    public InterWarehouseTransferResponse cancelTransfer(@PathVariable Long id,
                                                         @Valid @RequestBody InterWarehouseTransferReasonRequest request) {
        return transferService.cancelTransfer(id, request, currentUser());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER')")
    @Operation(summary = "Source warehouse manager approves and reserves stock")
    public InterWarehouseTransferResponse approveTransfer(@PathVariable Long id) {
        return transferService.approveTransfer(id, currentUser());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER')")
    @Operation(summary = "Source warehouse manager rejects NEW transfer")
    public InterWarehouseTransferResponse rejectTransfer(@PathVariable Long id,
                                                         @Valid @RequestBody InterWarehouseTransferReasonRequest request) {
        return transferService.rejectTransfer(id, request, currentUser());
    }

    @PostMapping("/{id}/trip")
    @PreAuthorize("hasRole('DISPATCHER')")
    @Operation(summary = "Dispatcher assigns dedicated TRANSFER trip")
    public InterWarehouseTransferResponse assignTrip(@PathVariable Long id,
                                                     @Valid @RequestBody InterWarehouseTransferTripAssignRequest request) {
        return transferService.assignTrip(id, request, currentUser());
    }

    @PostMapping("/{id}/ship")
    @PreAuthorize("hasAnyRole('STOREKEEPER','ADMIN','CEO')")
    @Operation(summary = "Source storekeeper loads exact planned quantity")
    public InterWarehouseTransferResponse shipTransfer(@PathVariable Long id) {
        return transferService.shipTransfer(id, currentUser());
    }

    @PostMapping("/{id}/unship")
    @PreAuthorize("hasAnyRole('STOREKEEPER','WAREHOUSE_MANAGER','ADMIN','CEO')")
    @Operation(summary = "Unload transfer before canceling APPROVED transfer")
    public InterWarehouseTransferResponse unshipTransfer(@PathVariable Long id) {
        return transferService.unshipTransfer(id, currentUser());
    }

    @PostMapping("/{id}/outbound-qc")
    @PreAuthorize("hasAnyRole('STOREKEEPER','WAREHOUSE_MANAGER','ADMIN','CEO')")
    @Operation(summary = "Record outbound QC results for the transfer")
    public InterWarehouseTransferResponse recordOutboundQc(@PathVariable Long id,
                                                           @Valid @RequestBody OutboundQcRequest request) {
        return transferService.recordOutboundQc(id, request, currentUser());
    }

    @PostMapping("/{id}/photo-evidence")
    @PreAuthorize("hasAnyRole('STOREKEEPER','WAREHOUSE_STAFF','WAREHOUSE_MANAGER','DRIVER','ADMIN','CEO')")
    @Operation(summary = "Upload transfer photo evidence and return a short photo reference")
    public TransferPhotoUploadResponse uploadPhotoEvidence(@PathVariable Long id,
                                                           @RequestParam MultipartFile file) {
        return transferService.uploadPhotoEvidence(id, file, currentUser());
    }

    @PostMapping("/{id}/load-handover")
    @PreAuthorize("hasAnyRole('STOREKEEPER','WAREHOUSE_MANAGER','ADMIN','CEO')")
    @Operation(summary = "Record load handover photo for the transfer")
    public InterWarehouseTransferResponse loadHandover(@PathVariable Long id,
                                                       @Valid @RequestBody LoadHandoverRequest request) {
        return transferService.loadHandover(id, request, currentUser());
    }

    @PostMapping("/{id}/depart")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Assigned driver confirms departure and moves stock to in-transit")
    public InterWarehouseTransferResponse departTransfer(@PathVariable Long id) {
        return transferService.departTransfer(id, currentUser());
    }

    @PostMapping("/{id}/driver-arrive")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Assigned driver confirms arrival at destination warehouse")
    public InterWarehouseTransferResponse driverArrive(@PathVariable Long id) {
        return transferService.driverArrive(id, currentUser());
    }

    @PostMapping("/{id}/receiving-handover")
    @PreAuthorize("hasAnyRole('STOREKEEPER','WAREHOUSE_STAFF','WAREHOUSE_MANAGER','ADMIN','CEO')")
    @Operation(summary = "Record arrival handover check and photo at destination warehouse")
    public InterWarehouseTransferResponse receivingHandover(@PathVariable Long id,
                                                            @Valid @RequestBody LoadHandoverRequest request) {
        return transferService.receivingHandover(id, request, currentUser());
    }

    @PutMapping("/{id}/receive-count")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF','ADMIN','CEO')")
    @Operation(summary = "Destination worker records initial received quantities")
    public InterWarehouseTransferResponse receiveCount(@PathVariable Long id,
                                                       @Valid @RequestBody InterWarehouseTransferReceiveCountRequest request) {
        return transferService.receiveCount(id, request, currentUser());
    }

    @PutMapping("/{id}/receive-check")
    @PreAuthorize("hasAnyRole('STOREKEEPER','ADMIN','CEO')")
    @Operation(summary = "Destination storekeeper checks count and QC")
    public InterWarehouseTransferResponse receiveCheck(@PathVariable Long id,
                                                       @Valid @RequestBody InterWarehouseTransferReceiveCheckRequest request) {
        return transferService.receiveCheck(id, request, currentUser());
    }

    @PostMapping("/{id}/final-receive")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER','ADMIN','CEO')")
    @Operation(summary = "Destination warehouse manager finalizes transfer")
    public InterWarehouseTransferResponse finalReceive(@PathVariable Long id,
                                                       @Valid @RequestBody InterWarehouseTransferFinalReceiveRequest request) {
        return transferService.finalReceive(id, request, currentUser());
    }

    @PostMapping("/{id}/return-to-source")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER','ADMIN','CEO','PLANNER')")
    @Operation(summary = "Mark the in-transit transfer as returning/returned to source warehouse")
    public InterWarehouseTransferResponse returnToSource(@PathVariable Long id,
                                                         @Valid @RequestBody TransferReturnRequest request) {
        return transferService.returnToSource(id, request, currentUser());
    }

    @PostMapping("/{id}/quarantine-reject")
    @PreAuthorize("hasAnyRole('STOREKEEPER','WAREHOUSE_MANAGER','ADMIN','CEO')")
    @Operation(summary = "Storekeeper or Manager rejects and quarantines the entire transfer")
    public InterWarehouseTransferResponse quarantineReject(@PathVariable Long id,
                                                           @Valid @RequestBody InterWarehouseTransferRejectRequest request) {
        return transferService.quarantineReject(id, request, currentUser());
    }

    @PostMapping("/{id}/request-return")
    @PreAuthorize("hasAnyRole('STOREKEEPER','ADMIN','CEO')")
    @Operation(summary = "Storekeeper reports wrong SKU delivery (request return to source)")
    public InterWarehouseTransferResponse requestReturn(@PathVariable Long id,
                                                        @Valid @RequestBody TransferReturnRequest request) {
        return transferService.requestReturn(id, request, currentUser());
    }

    @PostMapping("/{id}/approve-return")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER','ADMIN','CEO')")
    @Operation(summary = "Manager approves the return to source")
    public InterWarehouseTransferResponse approveReturn(@PathVariable Long id) {
        return transferService.approveReturn(id, currentUser());
    }

    @PostMapping("/{id}/reject-return")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER','ADMIN','CEO')")
    @Operation(summary = "Manager rejects the return request")
    public InterWarehouseTransferResponse rejectReturn(@PathVariable Long id,
                                                       @Valid @RequestBody TransferReturnRejectRequest request) {
        return transferService.rejectReturn(id, request, currentUser());
    }

    @PostMapping("/{id}/return-depart")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Assigned driver confirms departure for return leg")
    public InterWarehouseTransferResponse returnDepart(@PathVariable Long id) {
        return transferService.returnDepart(id, currentUser());
    }

    @PostMapping("/{id}/return-arrive")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Assigned driver confirms arrival at source warehouse for return leg")
    public InterWarehouseTransferResponse returnArrive(@PathVariable Long id) {
        return transferService.returnArrive(id, currentUser());
    }

    @PostMapping("/{id}/return-handover")
    @PreAuthorize("hasAnyRole('STOREKEEPER','WAREHOUSE_STAFF','WAREHOUSE_MANAGER','ADMIN','CEO')")
    @Operation(summary = "Record arrival handover check and photo at source warehouse for return leg")
    public InterWarehouseTransferResponse returnHandover(@PathVariable Long id,
                                                         @Valid @RequestBody LoadHandoverRequest request) {
        return transferService.returnHandover(id, request, currentUser());
    }

    private User currentUser() {
        return currentUserService.getRequiredCurrentUser();
    }
}
