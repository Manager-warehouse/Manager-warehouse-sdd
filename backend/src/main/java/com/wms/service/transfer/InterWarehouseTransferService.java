package com.wms.service.transfer;

import com.wms.dto.request.*;
import com.wms.dto.response.InterWarehouseTransferResponse;
import com.wms.entity.User;
import java.util.List;

public interface InterWarehouseTransferService {
    List<InterWarehouseTransferResponse> getAllTransfers(User actor);
    InterWarehouseTransferResponse getTransferById(Long id, User actor);
    InterWarehouseTransferResponse createTransfer(InterWarehouseTransferCreateRequest request, User actor);
    InterWarehouseTransferResponse updateTransfer(Long id, InterWarehouseTransferUpdateRequest request, User actor);
    InterWarehouseTransferResponse cancelTransfer(Long id, InterWarehouseTransferReasonRequest request, User actor);
    InterWarehouseTransferResponse approveTransfer(Long id, User actor);
    InterWarehouseTransferResponse rejectTransfer(Long id, InterWarehouseTransferReasonRequest request, User actor);
    InterWarehouseTransferResponse assignTrip(Long id, InterWarehouseTransferTripAssignRequest request, User actor);
    InterWarehouseTransferResponse shipTransfer(Long id, User actor);
    InterWarehouseTransferResponse unshipTransfer(Long id, User actor);
    InterWarehouseTransferResponse departTransfer(Long id, User actor);
    InterWarehouseTransferResponse receiveCount(Long id, InterWarehouseTransferReceiveCountRequest request, User actor);
    InterWarehouseTransferResponse receiveCheck(Long id, InterWarehouseTransferReceiveCheckRequest request, User actor);
    InterWarehouseTransferResponse finalReceive(Long id, InterWarehouseTransferFinalReceiveRequest request, User actor);
    InterWarehouseTransferResponse returnToSource(Long id, User actor);
    InterWarehouseTransferResponse quarantineReject(Long id, InterWarehouseTransferRejectRequest request, User actor);
    InterWarehouseTransferResponse requestReturn(Long id, TransferReturnRequest request, User actor);
    InterWarehouseTransferResponse approveReturn(Long id, User actor);
    InterWarehouseTransferResponse rejectReturn(Long id, TransferReturnRejectRequest request, User actor);
}
