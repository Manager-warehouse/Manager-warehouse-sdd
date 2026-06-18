package com.wms.controller;

import com.wms.dto.request.TransferCreateRequest;
import com.wms.dto.request.TransferFinalReceiveRequest;
import com.wms.dto.request.TransferReasonRequest;
import com.wms.dto.request.TransferReceiveCheckRequest;
import com.wms.dto.request.TransferReceiveCountRequest;
import com.wms.dto.request.TransferTripAssignRequest;
import com.wms.dto.request.TransferUpdateRequest;
import com.wms.dto.response.TransferResponse;
import com.wms.entity.User;
import com.wms.service.CurrentUserService;
import com.wms.service.TransferService;
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

@RestController
@RequestMapping("/api/v1/transfers")
@Tag(name = "Inter-Warehouse Transfers", description = "Spec 005 transfer planning, shipping, and receiving")
public class TransferController {

    private final TransferService transferService;
    private final CurrentUserService currentUserService;

    public TransferController(TransferService transferService,
                              CurrentUserService currentUserService) {
        this.transferService = transferService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CEO','PLANNER','DISPATCHER','WAREHOUSE_MANAGER','STOREKEEPER','WAREHOUSE_STAFF','DRIVER')")
    @Operation(summary = "List inter-warehouse transfers")
    public List<TransferResponse> getAllTransfers() {
        return transferService.getAllTransfers(currentUser());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CEO','PLANNER','DISPATCHER','WAREHOUSE_MANAGER','STOREKEEPER','WAREHOUSE_STAFF','DRIVER')")
    @Operation(summary = "Get inter-warehouse transfer detail")
    public TransferResponse getTransferById(@PathVariable Long id) {
        return transferService.getTransferById(id, currentUser());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Create NEW transfer from external instruction")
    public TransferResponse createTransfer(@Valid @RequestBody TransferCreateRequest request) {
        return transferService.createTransfer(request, currentUser());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Replace NEW transfer details and item list")
    public TransferResponse updateTransfer(@PathVariable Long id,
                                           @Valid @RequestBody TransferUpdateRequest request) {
        return transferService.updateTransfer(id, request, currentUser());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PLANNER','WAREHOUSE_MANAGER')")
    @Operation(summary = "Cancel NEW or unshipped APPROVED transfer")
    public TransferResponse cancelTransfer(@PathVariable Long id,
                                           @RequestBody TransferReasonRequest request) {
        return transferService.cancelTransfer(id, request, currentUser());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER')")
    @Operation(summary = "Source warehouse manager approves and reserves stock")
    public TransferResponse approveTransfer(@PathVariable Long id) {
        return transferService.approveTransfer(id, currentUser());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER')")
    @Operation(summary = "Source warehouse manager rejects NEW transfer")
    public TransferResponse rejectTransfer(@PathVariable Long id,
                                           @RequestBody TransferReasonRequest request) {
        return transferService.rejectTransfer(id, request, currentUser());
    }

    @PostMapping("/{id}/trip")
    @PreAuthorize("hasRole('DISPATCHER')")
    @Operation(summary = "Dispatcher assigns dedicated TRANSFER trip")
    public TransferResponse assignTrip(@PathVariable Long id,
                                       @Valid @RequestBody TransferTripAssignRequest request) {
        return transferService.assignTrip(id, request, currentUser());
    }

    @PostMapping("/{id}/ship")
    @PreAuthorize("hasRole('STOREKEEPER')")
    @Operation(summary = "Source storekeeper loads exact planned quantity")
    public TransferResponse shipTransfer(@PathVariable Long id) {
        return transferService.shipTransfer(id, currentUser());
    }

    @PostMapping("/{id}/unship")
    @PreAuthorize("hasAnyRole('STOREKEEPER','WAREHOUSE_MANAGER')")
    @Operation(summary = "Unload transfer before canceling APPROVED transfer")
    public TransferResponse unshipTransfer(@PathVariable Long id) {
        return transferService.unshipTransfer(id, currentUser());
    }

    @PostMapping("/{id}/depart")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Assigned driver confirms departure and moves stock to in-transit")
    public TransferResponse departTransfer(@PathVariable Long id) {
        return transferService.departTransfer(id, currentUser());
    }

    @PutMapping("/{id}/receive-count")
    @PreAuthorize("hasRole('WAREHOUSE_STAFF')")
    @Operation(summary = "Destination worker records initial received quantities")
    public TransferResponse receiveCount(@PathVariable Long id,
                                         @Valid @RequestBody TransferReceiveCountRequest request) {
        return transferService.receiveCount(id, request, currentUser());
    }

    @PutMapping("/{id}/receive-check")
    @PreAuthorize("hasRole('STOREKEEPER')")
    @Operation(summary = "Destination storekeeper checks count and QC")
    public TransferResponse receiveCheck(@PathVariable Long id,
                                         @Valid @RequestBody TransferReceiveCheckRequest request) {
        return transferService.receiveCheck(id, request, currentUser());
    }

    @PostMapping("/{id}/final-receive")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER')")
    @Operation(summary = "Destination warehouse manager finalizes transfer")
    public TransferResponse finalReceive(@PathVariable Long id,
                                         @RequestBody TransferFinalReceiveRequest request) {
        return transferService.finalReceive(id, request, currentUser());
    }

    private User currentUser() {
        return currentUserService.getRequiredCurrentUser();
    }
}
