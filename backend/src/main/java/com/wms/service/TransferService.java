package com.wms.service;

import com.wms.dto.request.TransferCreateRequest;
import com.wms.dto.request.TransferFinalReceiveRequest;
import com.wms.dto.request.TransferReasonRequest;
import com.wms.dto.request.TransferReceiveCheckRequest;
import com.wms.dto.request.TransferReceiveCountRequest;
import com.wms.dto.request.TransferTripAssignRequest;
import com.wms.dto.request.TransferUpdateRequest;
import com.wms.dto.response.TransferResponse;
import com.wms.entity.User;
import java.util.List;

public interface TransferService {
    List<TransferResponse> getAllTransfers(User actor);
    TransferResponse getTransferById(Long id, User actor);
    TransferResponse createTransfer(TransferCreateRequest request, User actor);
    TransferResponse updateTransfer(Long id, TransferUpdateRequest request, User actor);
    TransferResponse cancelTransfer(Long id, TransferReasonRequest request, User actor);
    TransferResponse approveTransfer(Long id, User actor);
    TransferResponse rejectTransfer(Long id, TransferReasonRequest request, User actor);
    TransferResponse assignTrip(Long id, TransferTripAssignRequest request, User actor);
    TransferResponse shipTransfer(Long id, User actor);
    TransferResponse unshipTransfer(Long id, User actor);
    TransferResponse departTransfer(Long id, User actor);
    TransferResponse receiveCount(Long id, TransferReceiveCountRequest request, User actor);
    TransferResponse receiveCheck(Long id, TransferReceiveCheckRequest request, User actor);
    TransferResponse finalReceive(Long id, TransferFinalReceiveRequest request, User actor);
}
