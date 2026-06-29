package com.wms.aop;

import com.wms.entity.Inventory;
import com.wms.entity.WarehouseProductReservation;
import com.wms.service.StockAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryStockAlertAspect {

    private final StockAlertService stockAlertService;

    // 1. Intercept Inventory save
    @AfterReturning(
            pointcut = "execution(* com.wms.repository.InventoryRepository.save(..)) && args(inventory)",
            returning = "result"
    )
    public void afterInventorySave(JoinPoint joinPoint, Inventory inventory, Object result) {
        triggerCheck(inventory);
    }

    // 2. Intercept Inventory saveAll
    @SuppressWarnings("unchecked")
    @AfterReturning(
            pointcut = "execution(* com.wms.repository.InventoryRepository.saveAll(..)) && args(entities)",
            returning = "result"
    )
    public void afterInventorySaveAll(JoinPoint joinPoint, Object entities, Object result) {
        if (entities instanceof Collection) {
            Collection<Inventory> inventoryList = (Collection<Inventory>) entities;
            for (Inventory inventory : inventoryList) {
                triggerCheck(inventory);
            }
        }
    }

    // 3. Intercept WarehouseProductReservation save
    @AfterReturning(
            pointcut = "execution(* com.wms.repository.WarehouseProductReservationRepository.save(..)) && args(reservation)",
            returning = "result"
    )
    public void afterReservationSave(JoinPoint joinPoint, WarehouseProductReservation reservation, Object result) {
        triggerCheck(reservation);
    }

    // 4. Intercept WarehouseProductReservation saveAll
    @SuppressWarnings("unchecked")
    @AfterReturning(
            pointcut = "execution(* com.wms.repository.WarehouseProductReservationRepository.saveAll(..)) && args(entities)",
            returning = "result"
    )
    public void afterReservationSaveAll(JoinPoint joinPoint, Object entities, Object result) {
        if (entities instanceof Collection) {
            Collection<WarehouseProductReservation> reservationList = (Collection<WarehouseProductReservation>) entities;
            for (WarehouseProductReservation reservation : reservationList) {
                triggerCheck(reservation);
            }
        }
    }

    private void triggerCheck(Inventory inventory) {
        try {
            if (inventory != null && inventory.getWarehouse() != null && inventory.getProduct() != null) {
                stockAlertService.checkAndTriggerAlert(inventory.getWarehouse().getId(), inventory.getProduct().getId());
            }
        } catch (Exception e) {
            log.error("Failed to auto-check stock alert after Inventory update", e);
        }
    }

    private void triggerCheck(WarehouseProductReservation reservation) {
        try {
            if (reservation != null && reservation.getWarehouse() != null && reservation.getProduct() != null) {
                stockAlertService.checkAndTriggerAlert(reservation.getWarehouse().getId(), reservation.getProduct().getId());
            }
        } catch (Exception e) {
            log.error("Failed to auto-check stock alert after Reservation update", e);
        }
    }
}
