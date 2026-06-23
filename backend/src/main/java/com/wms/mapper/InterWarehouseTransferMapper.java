package com.wms.mapper;

import com.wms.dto.response.InterWarehouseTransferItemResponse;
import com.wms.dto.response.InterWarehouseTransferResponse;
import com.wms.entity.InterWarehouseTransfer;
import com.wms.entity.InterWarehouseTransferItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InterWarehouseTransferMapper {

    public InterWarehouseTransferResponse toResponse(InterWarehouseTransfer transfer,
                                                     List<InterWarehouseTransferItem> items,
                                                     boolean tripWarningActive,
                                                     boolean tripOverdue,
                                                     String tripWarningMessage) {
        List<InterWarehouseTransferItemResponse> itemResponses = items.stream()
                .map(InterWarehouseTransferItemResponse::from)
                .toList();
        return InterWarehouseTransferResponse.from(transfer, itemResponses, tripWarningActive, tripOverdue, tripWarningMessage);
    }
}
