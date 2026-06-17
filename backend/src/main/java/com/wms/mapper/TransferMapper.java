package com.wms.mapper;

import com.wms.dto.response.TransferItemResponse;
import com.wms.dto.response.TransferResponse;
import com.wms.entity.Transfer;
import com.wms.entity.TransferItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TransferMapper {

    public TransferResponse toResponse(Transfer transfer, List<TransferItem> items) {
        List<TransferItemResponse> itemResponses = items.stream()
                .map(TransferItemResponse::from)
                .toList();
        return TransferResponse.from(transfer, itemResponses);
    }
}
