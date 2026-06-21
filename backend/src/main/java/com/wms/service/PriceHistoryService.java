package com.wms.service;

import com.wms.dto.request.PriceHistoryCreateRequest;
import com.wms.dto.request.PriceHistoryUpdateRequest;
import com.wms.dto.response.PriceHistoryResponse;
import com.wms.dto.response.PriceImportResponse;
import com.wms.dto.response.ProductPriceHistoryResponse;
import com.wms.entity.PriceHistory;
import com.wms.entity.User;
import com.wms.enums.PriceHistoryStatus;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PriceHistoryService {

    PriceHistoryResponse create(PriceHistoryCreateRequest request, User actor);

    PriceHistoryResponse update(Long id, PriceHistoryUpdateRequest request, User actor);

    PriceHistoryResponse cancel(Long id, User actor);

    PriceHistoryResponse approve(Long id, User actor);

    PriceHistoryResponse getById(Long id, User actor);

    List<PriceHistoryResponse> getAll(Long productId, Long warehouseId, PriceHistoryStatus status);

    ProductPriceHistoryResponse getByProduct(Long productId);

    /** Price lookup for DO creation — scoped to the DO's warehouse. Returns empty if no APPROVED entry exists. */
    Optional<PriceHistory> lookupApproved(Long productId, Long warehouseId, LocalDate date);

    PriceImportResponse importFromExcel(MultipartFile file, User actor);
}
