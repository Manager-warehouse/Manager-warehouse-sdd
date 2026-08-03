package com.wms.service.warehouse_transfer;


import com.wms.dto.request.TransferRequestCreateRequest;
import com.wms.dto.request.TransferRequestRejectRequest;
import com.wms.dto.request.TransferRequestUpdateRequest;
import com.wms.dto.response.TransferRequestResponse;
import com.wms.dto.response.WarehouseStockLookupResponse;
import com.wms.entity.access_control.User;
import java.util.List;

public interface TransferRequestService {
    List<TransferRequestResponse> getAllRequests(User actor);
    TransferRequestResponse getRequestById(Long id, User actor);
    TransferRequestResponse createRequest(TransferRequestCreateRequest request, User actor);
    TransferRequestResponse updateRequest(Long id, TransferRequestUpdateRequest request, User actor);
    TransferRequestResponse cancelRequest(Long id, User actor);
    TransferRequestResponse submitRequest(Long id, User actor);
    TransferRequestResponse approveRequest(Long id, User actor);
    TransferRequestResponse rejectRequest(Long id, TransferRequestRejectRequest request, User actor);
    TransferRequestResponse convertToTransfer(Long id, User actor);
    List<WarehouseStockLookupResponse> stockLookup(Long productId, User actor);
}
