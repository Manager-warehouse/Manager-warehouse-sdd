package com.wms.aop;


import com.wms.entity.access_control.*;
import com.wms.entity.audit_trail.*;
import com.wms.entity.billing_payment.*;
import com.wms.entity.dealer_management.*;
import com.wms.entity.document_numbering.*;
import com.wms.entity.driver_management.*;
import com.wms.entity.fleet_management.*;
import com.wms.entity.notification_delivery.*;
import com.wms.entity.order_fulfillment.*;
import com.wms.entity.price_management.*;
import com.wms.entity.product_catalog.*;
import com.wms.entity.stock_control.*;
import com.wms.entity.stock_counting.*;
import com.wms.entity.stock_receiving.*;
import com.wms.entity.supplier_management.*;
import com.wms.entity.user_configuration.*;
import com.wms.entity.warehouse_location.*;
import com.wms.entity.warehouse_transfer.*;
import com.wms.enums.access_control.*;
import com.wms.enums.audit_trail.*;
import com.wms.enums.billing_payment.*;
import com.wms.enums.dealer_management.*;
import com.wms.enums.driver_management.*;
import com.wms.enums.fleet_management.*;
import com.wms.enums.notification_delivery.*;
import com.wms.enums.order_fulfillment.*;
import com.wms.enums.price_management.*;
import com.wms.enums.stock_control.*;
import com.wms.enums.stock_counting.*;
import com.wms.enums.stock_receiving.*;
import com.wms.enums.supplier_management.*;
import com.wms.enums.user_configuration.*;
import com.wms.enums.warehouse_location.*;
import com.wms.enums.warehouse_transfer.*;
import com.wms.entity.stock_control.Inventory;
import com.wms.entity.stock_control.WarehouseProductReservation;
import com.wms.service.reporting_alerting.StockAlertService;
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
