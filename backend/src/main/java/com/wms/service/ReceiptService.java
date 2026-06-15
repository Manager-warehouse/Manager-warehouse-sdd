package com.wms.service;

import com.wms.dto.request.ReceiptApproveRequest;
import com.wms.dto.request.ReceiptCreateRequest;
import com.wms.dto.request.ReceiptPutawayRequest;
import com.wms.dto.request.ReceiptQcRequest;
import com.wms.dto.request.ReceiptReceiveRequest;
import com.wms.dto.request.ReceiptRejectRequest;
import com.wms.dto.request.ReceiptRtvRequest;
import com.wms.dto.response.ReceiptDetailResponse;
import com.wms.dto.response.ReceiptResponse;
import com.wms.entity.User;
import java.util.List;

public interface ReceiptService {

    ReceiptDetailResponse create(ReceiptCreateRequest request, User actor);

    ReceiptDetailResponse get(Long id);

    List<ReceiptResponse> listByWarehouse(Long warehouseId);

    ReceiptDetailResponse receive(Long id, ReceiptReceiveRequest request, User actor);

    ReceiptDetailResponse qc(Long id, ReceiptQcRequest request, User actor);

    ReceiptDetailResponse approve(Long id, ReceiptApproveRequest request, User actor);

    ReceiptDetailResponse reject(Long id, ReceiptRejectRequest request, User actor);

    ReceiptDetailResponse rtv(Long id, ReceiptRtvRequest request, User actor);

    ReceiptDetailResponse putaway(Long id, ReceiptPutawayRequest request, User actor);
}
