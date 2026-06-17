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

    List<PriceHistoryResponse> getAll(Long productId, PriceHistoryStatus status);

    ProductPriceHistoryResponse getByProduct(Long productId);

    /** Price lookup for DO creation — returns empty if no APPROVED entry exists for that date. */
    Optional<PriceHistory> lookupApproved(Long productId, LocalDate date);

    PriceImportResponse importFromExcel(MultipartFile file, User actor);
}
