package com.wms.service;

import com.wms.dto.request.PriceHistoryCreateRequest;
import com.wms.dto.request.PriceHistoryUpdateRequest;
import com.wms.dto.response.PriceHistoryResponse;
import com.wms.entity.PriceHistory;
import com.wms.entity.Product;
import com.wms.entity.User;
import com.wms.entity.Warehouse;
import com.wms.enums.PriceHistoryStatus;
import com.wms.enums.UserRole;
import com.wms.exception.PriceHistoryException;
import com.wms.repository.NotificationRepository;
import com.wms.repository.PriceHistoryRepository;
import com.wms.repository.ProductRepository;
import com.wms.repository.UserRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.service.impl.PriceHistoryServiceImpl;
import com.wms.util.PartnerAuditUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceHistoryServiceTest {

    @Mock PriceHistoryRepository priceHistoryRepository;
    @Mock ProductRepository productRepository;
    @Mock WarehouseRepository warehouseRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock PartnerAuditUtil auditUtil;

    PriceHistoryServiceImpl service;

    User actor;
    Product product;
    Warehouse warehouse;

    @BeforeEach
    void setUp() {
        service = new PriceHistoryServiceImpl(
                priceHistoryRepository, productRepository,
                warehouseRepository, userRepository, notificationRepository, auditUtil);

        actor = new User();
        actor.setId(1L);
        actor.setFullName("Kế toán viên A");
        actor.setRole(UserRole.ACCOUNTANT);

        product = new Product();
        product.setId(10L);
        product.setSku("POT-001");
        product.setName("Nồi inox");

        warehouse = new Warehouse();
        warehouse.setId(1L);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_validRequest_returnsPending() {
        PriceHistoryCreateRequest req = buildCreateRequest(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(priceHistoryRepository.findApprovedOverlapping(eq(10L), anyLong(), any(), any(), isNull()))
                .thenReturn(List.of());
        when(userRepository.findByRole(UserRole.ACCOUNTANT_MANAGER)).thenReturn(List.of());
        PriceHistory saved = pendingPriceHistory(1L);
        when(priceHistoryRepository.save(any())).thenReturn(saved);

        PriceHistoryResponse resp = service.create(req, actor);

        assertThat(resp.getStatus()).isEqualTo("PENDING");
        verify(notificationRepository, never()).save(any()); // no managers to notify
    }

    @Test
    void create_invalidDateRange_throws() {
        PriceHistoryCreateRequest req = buildCreateRequest(
                LocalDate.of(2026, 7, 31), LocalDate.of(2026, 7, 1));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.create(req, actor))
                .isInstanceOf(PriceHistoryException.class)
                .hasMessageContaining("effective_date");
    }

    @Test
    void create_overlapsApproved_throws() {
        PriceHistoryCreateRequest req = buildCreateRequest(
                LocalDate.of(2026, 6, 15), LocalDate.of(2026, 7, 15));

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(priceHistoryRepository.findApprovedOverlapping(eq(10L), anyLong(), any(), any(), isNull()))
                .thenReturn(List.of(pendingPriceHistory(5L)));

        assertThatThrownBy(() -> service.create(req, actor))
                .isInstanceOf(PriceHistoryException.class)
                .hasMessageContaining("APPROVED");
    }

    // ── cancel ────────────────────────────────────────────────────────────────

    @Test
    void cancel_pending_setsStatusCancelled() {
        PriceHistory ph = pendingPriceHistory(1L);
        ph.setCreatedBy(actor);
        when(priceHistoryRepository.findById(1L)).thenReturn(Optional.of(ph));
        when(priceHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PriceHistoryResponse resp = service.cancel(1L, actor);

        assertThat(resp.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void cancel_approved_throws() {
        PriceHistory ph = pendingPriceHistory(1L);
        ph.setStatus(PriceHistoryStatus.APPROVED);
        ph.setCreatedBy(actor);
        when(priceHistoryRepository.findById(1L)).thenReturn(Optional.of(ph));

        assertThatThrownBy(() -> service.cancel(1L, actor))
                .isInstanceOf(PriceHistoryException.class)
                .hasMessageContaining("duyệt");
    }

    @Test
    void cancel_byDifferentUser_throws() {
        PriceHistory ph = pendingPriceHistory(1L);
        User other = new User();
        other.setId(99L);
        ph.setCreatedBy(other);
        when(priceHistoryRepository.findById(1L)).thenReturn(Optional.of(ph));

        assertThatThrownBy(() -> service.cancel(1L, actor))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    // ── approve ───────────────────────────────────────────────────────────────

    @Test
    void approve_pending_noOverlap_setsApproved() {
        PriceHistory ph = pendingPriceHistory(1L);
        User manager = new User();
        manager.setId(2L);
        manager.setFullName("KTT");
        when(priceHistoryRepository.findById(1L)).thenReturn(Optional.of(ph));
        when(priceHistoryRepository.findApprovedOverlapping(eq(10L), anyLong(), any(), any(), eq(1L)))
                .thenReturn(List.of());
        when(priceHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PriceHistoryResponse resp = service.approve(1L, manager);

        assertThat(resp.getStatus()).isEqualTo("APPROVED");
    }

    @Test
    void approve_raceConditionOverlap_throws() {
        PriceHistory ph = pendingPriceHistory(1L);
        when(priceHistoryRepository.findById(1L)).thenReturn(Optional.of(ph));
        when(priceHistoryRepository.findApprovedOverlapping(eq(10L), anyLong(), any(), any(), eq(1L)))
                .thenReturn(List.of(pendingPriceHistory(2L)));

        assertThatThrownBy(() -> service.approve(1L, actor))
                .isInstanceOf(PriceHistoryException.class)
                .hasMessageContaining("APPROVED");
    }

    @Test
    void approve_alreadyApproved_throws() {
        PriceHistory ph = pendingPriceHistory(1L);
        ph.setStatus(PriceHistoryStatus.APPROVED);
        when(priceHistoryRepository.findById(1L)).thenReturn(Optional.of(ph));

        assertThatThrownBy(() -> service.approve(1L, actor))
                .isInstanceOf(PriceHistoryException.class);
    }

    // ── lookupApproved ────────────────────────────────────────────────────────

    @Test
    void lookupApproved_found_returnsPrice() {
        PriceHistory ph = pendingPriceHistory(1L);
        ph.setStatus(PriceHistoryStatus.APPROVED);
        when(priceHistoryRepository.findApprovedAtDate(eq(10L), anyLong(), eq(LocalDate.of(2026, 6, 15))))
                .thenReturn(Optional.of(ph));

        Optional<PriceHistory> result = service.lookupApproved(10L, 1L, LocalDate.of(2026, 6, 15));

        assertThat(result).isPresent();
    }

    @Test
    void lookupApproved_notFound_returnsEmpty() {
        when(priceHistoryRepository.findApprovedAtDate(eq(10L), anyLong(), eq(LocalDate.of(2026, 7, 1))))
                .thenReturn(Optional.empty());

        Optional<PriceHistory> result = service.lookupApproved(10L, 1L, LocalDate.of(2026, 7, 1));

        assertThat(result).isEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private PriceHistoryCreateRequest buildCreateRequest(LocalDate effective, LocalDate end) {
        PriceHistoryCreateRequest req = new PriceHistoryCreateRequest();
        req.setProductId(10L);
        req.setWarehouseId(1L);
        req.setEffectiveDate(effective);
        req.setEndDate(end);
        req.setCostPrice(new BigDecimal("80000"));
        req.setSellingPrice(new BigDecimal("115000"));
        return req;
    }

    private PriceHistory pendingPriceHistory(Long id) {
        PriceHistory ph = new PriceHistory();
        ph.setId(id);
        ph.setProduct(product);
        ph.setEffectiveDate(LocalDate.of(2026, 7, 1));
        ph.setEndDate(LocalDate.of(2026, 7, 31));
        ph.setCostPrice(new BigDecimal("80000"));
        ph.setSellingPrice(new BigDecimal("115000"));
        ph.setStatus(PriceHistoryStatus.PENDING);
        ph.setCreatedBy(actor);
        ph.setCreatedAt(OffsetDateTime.now());
        ph.setUpdatedAt(OffsetDateTime.now());
        return ph;
    }
}
