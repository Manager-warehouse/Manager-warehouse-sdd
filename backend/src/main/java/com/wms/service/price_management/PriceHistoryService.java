package com.wms.service.price_management;


import com.wms.dto.request.PriceHistoryCreateRequest;
import com.wms.dto.request.PriceHistoryUpdateRequest;
import com.wms.dto.response.PriceHistoryResponse;
import com.wms.dto.response.PriceImportResponse;
import com.wms.dto.response.ProductPriceHistoryResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.price_management.PriceHistory;
import com.wms.enums.price_management.PriceHistoryStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

public interface PriceHistoryService {

    PriceHistoryResponse create(PriceHistoryCreateRequest request, User actor);

    PriceHistoryResponse update(Long id, PriceHistoryUpdateRequest request, User actor);

    PriceHistoryResponse cancel(Long id, User actor);

    PriceHistoryResponse approve(Long id, User actor);

    PriceHistoryResponse getById(Long id, User actor);

    List<PriceHistoryResponse> getAll(Long productId, Long warehouseId, PriceHistoryStatus status,
            LocalDate effectiveDateFrom, LocalDate effectiveDateTo, User actor);

    ProductPriceHistoryResponse getByProduct(Long productId, Long warehouseId, User actor);

    /** Price lookup for DO creation — scoped to the DO's warehouse. Returns empty if no APPROVED entry exists. */
    Optional<PriceHistory> lookupApproved(Long productId, Long warehouseId, LocalDate date);

    /**
     * targetWarehouseId, when provided, overrides the file's warehouse_code column for
     * every row — used when re-importing a file exported from another warehouse to clone
     * its prices into this one, without editing the column by hand.
     */
    PriceImportResponse importFromExcel(MultipartFile file, Long targetWarehouseId, User actor);
}
