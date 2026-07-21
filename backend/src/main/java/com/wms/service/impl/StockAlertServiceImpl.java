package com.wms.service.impl;

import com.wms.dto.response.StockAlertResponse;
import com.wms.entity.*;
import com.wms.enums.AlertType;
import com.wms.enums.UserRole;
import com.wms.enums.WarehouseType;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.*;
import com.wms.service.StockAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockAlertServiceImpl implements StockAlertService {

    private final StockAlertRepository stockAlertRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseProductReservationRepository warehouseProductReservationRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final UserRepository userRepository;
    private final UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void checkAndTriggerAlert(Long warehouseId, Long productId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId).orElse(null);
        Product product = productRepository.findById(productId).orElse(null);

        if (warehouse == null || product == null) {
            return;
        }

        // Bỏ qua kho ảo In-Transit
        if (warehouse.getType() == WarehouseType.IN_TRANSIT) {
            return;
        }

        // 1. Tính tồn khả dụng: available = total - reserved
        BigDecimal totalAvailable = inventoryRepository.sumValidAvailableQty(warehouseId, productId);
        if (totalAvailable == null) {
            totalAvailable = BigDecimal.ZERO;
        }

        BigDecimal reservedAtWarehouse = BigDecimal.ZERO;
        Optional<WarehouseProductReservation> resOpt = warehouseProductReservationRepository
                .findByWarehouseIdAndProductId(warehouseId, productId);
        if (resOpt.isPresent()) {
            reservedAtWarehouse = resOpt.get().getReservedQty();
        }

        BigDecimal availableQty = totalAvailable.subtract(reservedAtWarehouse);
        if (availableQty.compareTo(BigDecimal.ZERO) < 0) {
            availableQty = BigDecimal.ZERO;
        }

        // 2. Xác định ngưỡng cảnh báo
        BigDecimal reorderPoint = product.getReorderPoint();
        if (reorderPoint == null) {
            String defaultThresholdStr = systemConfigRepository.findByConfigKey("MIN_INVENTORY_WARNING_THRESHOLD")
                    .map(SystemConfig::getConfigValue)
                    .orElse("10");
            try {
                reorderPoint = new BigDecimal(defaultThresholdStr);
            } catch (Exception e) {
                reorderPoint = new BigDecimal("10");
            }
        }

        AlertType alertType = availableQty.compareTo(BigDecimal.ZERO) == 0 ? AlertType.OUT_OF_STOCK : AlertType.LOW_STOCK;
        Optional<StockAlert> activeAlertOpt = stockAlertRepository
                .findByWarehouseIdAndProductIdAndAlertTypeAndIsResolved(warehouseId, productId, AlertType.LOW_STOCK, false);
        
        if (activeAlertOpt.isEmpty()) {
            activeAlertOpt = stockAlertRepository
                    .findByWarehouseIdAndProductIdAndAlertTypeAndIsResolved(warehouseId, productId, AlertType.OUT_OF_STOCK, false);
        }

        // 3. So sánh tồn khả dụng với ngưỡng
        if (availableQty.compareTo(reorderPoint) < 0) {
            // Cần cảnh báo
            if (activeAlertOpt.isEmpty()) {
                // Tạo mới alert
                StockAlert alert = StockAlert.builder()
                        .warehouse(warehouse)
                        .product(product)
                        .currentQty(availableQty)
                        .reorderPoint(reorderPoint)
                        .alertType(alertType)
                        .isResolved(false)
                        .createdAt(OffsetDateTime.now())
                        .build();

                stockAlertRepository.save(alert);

                // Gửi in-app notification
                String message = String.format("[CẢNH BÁO] Sản phẩm %s (%s) tại %s đã giảm dưới định mức tối thiểu. Tồn khả dụng: %s / Ngưỡng: %s",
                        product.getName(), product.getSku(), warehouse.getName(), availableQty.toString(), reorderPoint.toString());

                sendNotifications(warehouseId, message, "LOW_STOCK", alert.getId());
                log.info("Triggered stock alert for product {} at warehouse {}", product.getSku(), warehouse.getCode());
            } else {
                // Cập nhật alert hiện tại
                StockAlert activeAlert = activeAlertOpt.get();
                activeAlert.setCurrentQty(availableQty);
                activeAlert.setAlertType(alertType);
                stockAlertRepository.save(activeAlert);
            }
        } else {
            // Tồn kho an toàn, giải quyết alert nếu có
            if (activeAlertOpt.isPresent()) {
                StockAlert activeAlert = activeAlertOpt.get();
                activeAlert.setIsResolved(true);
                activeAlert.setResolvedAt(OffsetDateTime.now());
                activeAlert.setCurrentQty(availableQty);
                stockAlertRepository.save(activeAlert);

                // Gửi thông báo đã giải quyết
                String message = String.format("[ĐÃ GIẢI QUYẾT] Sản phẩm %s (%s) tại %s đã được bổ sung đầy đủ. Tồn khả dụng: %s / Ngưỡng: %s",
                        product.getName(), product.getSku(), warehouse.getName(), availableQty.toString(), reorderPoint.toString());

                sendNotifications(warehouseId, message, "STOCK_RESOLVED", activeAlert.getId());
                log.info("Resolved stock alert for product {} at warehouse {}", product.getSku(), warehouse.getCode());
            }
        }
    }

    private void sendNotifications(Long warehouseId, String message, String type, Long alertId) {
        // Tìm Trưởng kho phụ trách
        List<User> managers = userWarehouseAssignmentRepository.findWarehouseManagersByWarehouseId(warehouseId);
        for (User manager : managers) {
            createNotification(manager, message, type, alertId);
        }

        // Tìm tất cả Planner
        List<User> planners = userRepository.findByRole(UserRole.PLANNER);
        for (User planner : planners) {
            createNotification(planner, message, type, alertId);
        }
    }

    private void createNotification(User recipient, String message, String type, Long alertId) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .referenceType("stock_alerts")
                .referenceId(alertId)
                .message(message)
                .isRead(false)
                .createdAt(OffsetDateTime.now())
                .build();
        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockAlertResponse> getLowStockAlerts(Long warehouseId, Long productId, Boolean isResolved, int page, int size, Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        // Phân quyền theo kho
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.PLANNER && user.getRole() != UserRole.CEO && user.getRole() != UserRole.WAREHOUSE_MANAGER) {
            List<Long> assignedWarehouseIds = userWarehouseAssignmentRepository.findWarehouseIdsByUserId(currentUserId);
            if (warehouseId != null) {
                if (!assignedWarehouseIds.contains(warehouseId)) {
                    throw new IllegalArgumentException("WAREHOUSE_SCOPE_FORBIDDEN");
                }
            } else {
                // Nếu user không truyền warehouseId, ta chỉ cho phép truy vấn các kho được gán
                if (assignedWarehouseIds.isEmpty()) {
                    return Page.empty();
                }
                // Hạn chế query chỉ lấy các kho được gán (ở đây đơn giản hóa là bắt buộc truyền hoặc tự gán)
                // Trong trường hợp này, nếu user chỉ gán 1 kho, ta gán warehouseId = kho đó
                if (assignedWarehouseIds.size() == 1) {
                    warehouseId = assignedWarehouseIds.get(0);
                } else {
                    // Nếu gán nhiều kho, để đơn giản ta có thể mở rộng Repository sau. 
                    // Trong khuôn khổ Sprint 1, ta tạm thời lấy kho đầu tiên hoặc check parameter.
                    warehouseId = assignedWarehouseIds.get(0);
                }
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<StockAlert> alertsPage = stockAlertRepository.findWithFilters(warehouseId, productId, isResolved, pageable);

        return alertsPage.map(this::mapToResponse);
    }

    private StockAlertResponse mapToResponse(StockAlert alert) {
        return StockAlertResponse.builder()
                .id(alert.getId())
                .warehouseId(alert.getWarehouse().getId())
                .warehouseName(alert.getWarehouse().getName())
                .productId(alert.getProduct().getId())
                .productSku(alert.getProduct().getSku())
                .productName(alert.getProduct().getName())
                .currentQty(alert.getCurrentQty())
                .reorderPoint(alert.getReorderPoint())
                .alertType(alert.getAlertType().name())
                .isResolved(alert.getIsResolved())
                .resolvedAt(alert.getResolvedAt())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}
