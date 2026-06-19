package com.wms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wms.dto.request.TransferCreateRequest;
import com.wms.dto.request.TransferFinalReceiveRequest;
import com.wms.dto.request.TransferItemRequest;
import com.wms.dto.request.TransferReasonRequest;
import com.wms.dto.request.TransferReceiveCheckItemRequest;
import com.wms.dto.request.TransferReceiveCheckRequest;
import com.wms.dto.request.TransferReceiveCountItemRequest;
import com.wms.dto.request.TransferReceiveCountRequest;
import com.wms.dto.request.TransferTripAssignRequest;
import com.wms.dto.request.TransferUpdateRequest;
import com.wms.dto.response.TransferResponse;
import com.wms.entity.Batch;
import com.wms.entity.Driver;
import com.wms.entity.Inventory;
import com.wms.entity.Product;
import com.wms.entity.Transfer;
import com.wms.entity.TransferAllocation;
import com.wms.entity.TransferItem;
import com.wms.entity.Trip;
import com.wms.entity.User;
import com.wms.entity.Vehicle;
import com.wms.entity.Warehouse;
import com.wms.entity.WarehouseLocation;
import com.wms.enums.AuditAction;
import com.wms.enums.DriverStatus;
import com.wms.enums.TransferStatus;
import com.wms.enums.TripStatus;
import com.wms.enums.TripType;
import com.wms.enums.UserRole;
import com.wms.enums.VehicleStatus;
import com.wms.exception.ResourceNotFoundException;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.repository.DriverRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.TransferAllocationRepository;
import com.wms.repository.TransferItemRepository;
import com.wms.repository.TransferRepository;
import com.wms.repository.TripRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.VehicleRepository;
import com.wms.repository.WarehouseLocationRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.service.impl.TransferServiceImpl;
import com.wms.util.PartnerAuditUtil;
import jakarta.persistence.EntityManager;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransferServiceImplTest {

    private TransferRepository transferRepository;
    private TransferItemRepository transferItemRepository;
    private TransferAllocationRepository allocationRepository;
    private InventoryRepository inventoryRepository;
    private WarehouseRepository warehouseRepository;
    private WarehouseLocationRepository locationRepository;
    private UserWarehouseAssignmentRepository assignmentRepository;
    private VehicleRepository vehicleRepository;
    private DriverRepository driverRepository;
    private TripRepository tripRepository;
    private TrackingAuditUtil auditUtil;
    private EntityManager entityManager;
    private TransferServiceImpl service;

    private Warehouse sourceWarehouse;
    private Warehouse destinationWarehouse;
    private Warehouse transitWarehouse;
    private WarehouseLocation sourceLocation;
    private WarehouseLocation transitLocation;
    private WarehouseLocation destinationLocation;
    private WarehouseLocation quarantineLocation;
    private Product product;
    private User planner;
    private User sourceManager;
    private User destinationWorker;
    private User destinationStorekeeper;
    private User destinationManager;
    private User dispatcher;
    private User driverUser;
    private Vehicle vehicle;
    private Driver driver;
    private Transfer transfer;
    private TransferItem transferItem;
    private final List<TransferItem> transferItems = new ArrayList<>();
    private Inventory sourceInventory;
    private Inventory transitInventory;
    private Inventory destinationInventory;
    private Inventory quarantineInventory;
    private Trip transferTrip;
    private final Map<Long, List<Long>> assignments = new HashMap<>();
    private final TrackingAllocationRepository allocationState = new TrackingAllocationRepository();

    @BeforeEach
    void setUp() {
        sourceWarehouse = warehouse(1L, "HP-01");
        destinationWarehouse = warehouse(2L, "HN-01");
        transitWarehouse = warehouse(99L, "IN_TRANSIT");
        sourceLocation = location(10L, sourceWarehouse, "HP-01-B01", false);
        transitLocation = location(11L, transitWarehouse, "INT-01", false);
        destinationLocation = location(12L, destinationWarehouse, "HN-01-B01", false);
        quarantineLocation = location(13L, destinationWarehouse, "HN-01-Q01", true);
        product = product();
        planner = user(7L, UserRole.PLANNER);
        sourceManager = user(8L, UserRole.WAREHOUSE_MANAGER);
        destinationWorker = user(9L, UserRole.WAREHOUSE_STAFF);
        destinationStorekeeper = user(10L, UserRole.STOREKEEPER);
        destinationManager = user(11L, UserRole.WAREHOUSE_MANAGER);
        dispatcher = user(13L, UserRole.DISPATCHER);
        driverUser = user(12L, UserRole.DRIVER);
        vehicle = vehicle();
        driver = driver();
        transfer = transfer();
        transferItem = transferItem();
        transferItems.add(transferItem);
        sourceInventory = inventory(sourceWarehouse, sourceLocation, new BigDecimal("20.00"));
        transitInventory = null;
        destinationInventory = null;
        quarantineInventory = null;
        transferTrip = null;

        assignments.put(sourceManager.getId(), List.of(sourceWarehouse.getId()));
        assignments.put(destinationWorker.getId(), List.of(destinationWarehouse.getId()));
        assignments.put(destinationStorekeeper.getId(), List.of(destinationWarehouse.getId()));
        assignments.put(destinationManager.getId(), List.of(destinationWarehouse.getId()));
        assignments.put(dispatcher.getId(), List.of(sourceWarehouse.getId()));
        assignments.put(planner.getId(), List.of());
        assignments.put(driverUser.getId(), List.of(sourceWarehouse.getId(), destinationWarehouse.getId()));

        transferRepository = proxy(TransferRepository.class, new TransferRepoHandler());
        transferItemRepository = proxy(TransferItemRepository.class, new TransferItemRepoHandler());
        allocationRepository = proxy(TransferAllocationRepository.class, allocationState);
        inventoryRepository = proxy(InventoryRepository.class, new InventoryRepoHandler());
        warehouseRepository = proxy(WarehouseRepository.class, new WarehouseRepoHandler());
        locationRepository = proxy(WarehouseLocationRepository.class, new LocationRepoHandler());
        assignmentRepository = proxy(UserWarehouseAssignmentRepository.class, new AssignmentRepoHandler());
        vehicleRepository = proxy(VehicleRepository.class, new VehicleRepoHandler());
        driverRepository = proxy(DriverRepository.class, new DriverRepoHandler());
        tripRepository = proxy(TripRepository.class, new TripRepoHandler());
        auditUtil = new TrackingAuditUtil();
        entityManager = proxy(EntityManager.class, new EntityManagerHandler());

        service = new TransferServiceImpl(transferRepository, transferItemRepository, allocationRepository,
                inventoryRepository, warehouseRepository, locationRepository, assignmentRepository,
                vehicleRepository, driverRepository, tripRepository, new com.wms.mapper.TransferMapper(),
                auditUtil, entityManager);
    }

    @Test
    void plannerLifecycle_createUpdateCancelNewWorks() {
        TransferCreateRequest createRequest = new TransferCreateRequest(
                "CTM-20260617-01",
                sourceWarehouse.getId(),
                destinationWarehouse.getId(),
                LocalDate.of(2026, 6, 17),
                LocalDate.of(2026, 6, 18),
                "manual instruction",
                List.of(new TransferItemRequest(product.getId(), sourceLocation.getId(), destinationLocation.getId(), new BigDecimal("4.00"))));

        TransferResponse created = service.createTransfer(createRequest, planner);
        assertThat(created.status()).isEqualTo(TransferStatus.NEW);
        assertThat(created.externalInstructionCode()).isEqualTo("CTM-20260617-01");

        TransferUpdateRequest updateRequest = new TransferUpdateRequest(
                "CTM-20260617-01",
                sourceWarehouse.getId(),
                destinationWarehouse.getId(),
                LocalDate.of(2026, 6, 17),
                LocalDate.of(2026, 6, 19),
                "manual instruction updated",
                List.of(new TransferItemRequest(product.getId(), sourceLocation.getId(), destinationLocation.getId(), new BigDecimal("6.00"))));
        TransferResponse updated = service.updateTransfer(1L, updateRequest, planner);
        assertThat(updated.plannedDate()).isEqualTo(LocalDate.of(2026, 6, 19));
        assertThat(updated.items()).hasSize(1);
        assertThat(updated.items().get(0).plannedQty()).isEqualByComparingTo("6.00");

        TransferResponse cancelled = service.cancelTransfer(1L, new TransferReasonRequest("Planner cancel"), planner);
        assertThat(cancelled.status()).isEqualTo(TransferStatus.CANCELLED);
    }

    @Test
    void sourceFlow_approveAssignShipUnshipDepartWorks() {
        transfer.setCreatedBy(planner);
        TransferResponse approved = service.approveTransfer(1L, sourceManager);
        assertThat(approved.status()).isEqualTo(TransferStatus.APPROVED);
        assertThat(sourceInventory.getReservedQty()).isEqualByComparingTo("5.00");
        assertThat(allocationState.saved).hasSize(1);
        assertThat(auditUtil.lastAction).isEqualTo(AuditAction.TRANSFER_APPROVE);

        TransferResponse assigned = service.assignTrip(1L, new TransferTripAssignRequest(vehicle.getId(), driver.getId(), LocalDate.of(2026, 6, 19)), dispatcher);
        assertThat(assigned.tripId()).isNotNull();
        assertThat(transfer.getTrip()).isNotNull();

        TransferResponse shipped = service.shipTransfer(1L, sourceManager);
        assertThat(shipped.items().get(0).sentQty()).isEqualByComparingTo("5.00");

        TransferResponse unshipped = service.unshipTransfer(1L, sourceManager);
        assertThat(unshipped.items().get(0).sentQty()).isNull();

        service.shipTransfer(1L, sourceManager);
        TransferResponse departed = service.departTransfer(1L, driverUser);
        assertThat(departed.status()).isEqualTo(TransferStatus.IN_TRANSIT);
        assertThat(sourceInventory.getTotalQty()).isEqualByComparingTo("15.00");
        assertThat(transitInventory).isNotNull();
        assertThat(transitInventory.getTotalQty()).isEqualByComparingTo("5.00");
        assertThat(transfer.getTrip().getStatus()).isEqualTo(TripStatus.IN_TRANSIT);
    }

    @Test
    void assignTrip_requiresDriverFromSourceWarehouse() {
        service.approveTransfer(1L, sourceManager);
        assignments.put(driverUser.getId(), List.of(destinationWarehouse.getId()));

        assertThatThrownBy(() -> service.assignTrip(
                1L,
                new TransferTripAssignRequest(vehicle.getId(), driver.getId(), LocalDate.of(2026, 6, 19)),
                dispatcher))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("DRIVER_SOURCE_WAREHOUSE_REQUIRED");
    }

    @Test
    void assignTrip_requiresDispatcherAndVehicleFromSourceWarehouse() {
        service.approveTransfer(1L, sourceManager);

        assignments.put(dispatcher.getId(), List.of(destinationWarehouse.getId()));
        assertThatThrownBy(() -> service.assignTrip(
                1L,
                new TransferTripAssignRequest(vehicle.getId(), driver.getId(), LocalDate.of(2026, 6, 19)),
                dispatcher))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("WAREHOUSE_SCOPE_REQUIRED");

        assignments.put(dispatcher.getId(), List.of(sourceWarehouse.getId()));
        vehicle.setWarehouse(destinationWarehouse);
        assertThatThrownBy(() -> service.assignTrip(
                1L,
                new TransferTripAssignRequest(vehicle.getId(), driver.getId(), LocalDate.of(2026, 6, 19)),
                dispatcher))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("VEHICLE_SOURCE_WAREHOUSE_REQUIRED");
    }

    @Test
    void destinationWorker_onlySeesTransfersForAssignedDestinationWarehouse() {
        transfer.setStatus(TransferStatus.IN_TRANSIT);

        assertThat(service.getAllTransfers(destinationWorker)).hasSize(1);

        assignments.put(destinationWorker.getId(), List.of(sourceWarehouse.getId()));
        assertThat(service.getAllTransfers(destinationWorker)).isEmpty();
        assertThatThrownBy(() -> service.getTransferById(transfer.getId(), destinationWorker))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("WAREHOUSE_SCOPE_REQUIRED");
    }

    @Test
    void destinationFlow_receiveCountCheckAndFinalConfirmWorks() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new TransferTripAssignRequest(vehicle.getId(), driver.getId(), LocalDate.of(2026, 6, 19)), dispatcher);
        service.shipTransfer(1L, sourceManager);
        service.departTransfer(1L, driverUser);

        TransferResponse counted = service.receiveCount(1L, new TransferReceiveCountRequest(List.of(
                new TransferReceiveCountItemRequest(transferItem.getId(), new BigDecimal("4.00"), "1 missing")
        )), destinationWorker);
        assertThat(counted.items().get(0).workerReceivedQty()).isEqualByComparingTo("4.00");

        TransferResponse checked = service.receiveCheck(1L, new TransferReceiveCheckRequest(List.of(
                new TransferReceiveCheckItemRequest(
                        transferItem.getId(),
                        new BigDecimal("4.00"),
                        new BigDecimal("3.00"),
                        new BigDecimal("1.00"),
                        destinationLocation.getId(),
                        "checker adjusted count",
                        "one damaged")
        )), destinationStorekeeper);
        assertThat(checked.items().get(0).receivedQty()).isEqualByComparingTo("4.00");
        assertThat(checked.items().get(0).qcPassedQty()).isEqualByComparingTo("3.00");

        TransferResponse completed = service.finalReceive(1L, new TransferFinalReceiveRequest("shortage due to missing unit"), destinationManager);
        assertThat(completed.status()).isEqualTo(TransferStatus.COMPLETED_WITH_DISCREPANCY);
        assertThat(destinationInventory).isNotNull();
        assertThat(destinationInventory.getTotalQty()).isEqualByComparingTo("3.00");
        assertThat(quarantineInventory).isNotNull();
        assertThat(quarantineInventory.getTotalQty()).isEqualByComparingTo("1.00");
    }

    @Test
    void receiveCount_overReceipt_isBlocked() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new TransferTripAssignRequest(vehicle.getId(), driver.getId(), LocalDate.of(2026, 6, 19)), dispatcher);
        service.shipTransfer(1L, sourceManager);
        service.departTransfer(1L, driverUser);

        assertThatThrownBy(() -> service.receiveCount(1L, new TransferReceiveCountRequest(List.of(
                new TransferReceiveCountItemRequest(transferItem.getId(), new BigDecimal("6.00"), "too many")
        )), destinationWorker))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("OVER_RECEIPT_BLOCKED");
    }

    private Transfer transfer() {
        Transfer value = new Transfer();
        value.setId(1L);
        value.setTransferNumber("TRF-20260617-0001");
        value.setExternalInstructionCode("CTM-0001");
        value.setSourceWarehouse(sourceWarehouse);
        value.setDestinationWarehouse(destinationWarehouse);
        value.setStatus(TransferStatus.NEW);
        value.setCreatedBy(planner);
        value.setDocumentDate(LocalDate.of(2026, 6, 17));
        value.setCreatedAt(OffsetDateTime.now());
        value.setUpdatedAt(OffsetDateTime.now());
        return value;
    }

    private TransferItem transferItem() {
        TransferItem item = new TransferItem();
        item.setId(101L);
        item.setTransfer(transfer);
        item.setProduct(product);
        item.setSourceLocation(sourceLocation);
        item.setDestinationLocation(destinationLocation);
        item.setPlannedQty(new BigDecimal("5.00"));
        return item;
    }

    private Inventory inventory(Warehouse warehouse, WarehouseLocation location, BigDecimal totalQty) {
        Batch batch = new Batch();
        batch.setId(301L);
        Inventory value = new Inventory();
        value.setId(401L);
        value.setWarehouse(warehouse);
        value.setProduct(product);
        value.setBatch(batch);
        value.setLocation(location);
        value.setTotalQty(totalQty);
        value.setReservedQty(BigDecimal.ZERO);
        value.setCostPrice(new BigDecimal("100.00"));
        value.setVersion(0);
        value.setUpdatedAt(OffsetDateTime.now());
        return value;
    }

    private Product product() {
        Product value = new Product();
        value.setId(21L);
        value.setSku("SKU-001");
        value.setName("Nồi inox");
        return value;
    }

    private Warehouse warehouse(Long id, String code) {
        Warehouse value = new Warehouse();
        value.setId(id);
        value.setCode(code);
        value.setName(code);
        return value;
    }

    private WarehouseLocation location(Long id, Warehouse warehouse, String code, boolean quarantine) {
        WarehouseLocation value = new WarehouseLocation();
        value.setId(id);
        value.setWarehouse(warehouse);
        value.setCode(code);
        value.setIsQuarantine(quarantine);
        value.setIsActive(true);
        return value;
    }

    private User user(Long id, UserRole role) {
        User value = new User();
        value.setId(id);
        value.setRole(role);
        return value;
    }

    private Vehicle vehicle() {
        Vehicle value = new Vehicle();
        value.setId(501L);
        value.setPlateNumber("15C-234.56");
        value.setVehicleType("Truck");
        value.setMaxWeightKg(new BigDecimal("1500.00"));
        value.setWarehouse(sourceWarehouse);
        value.setStatus(VehicleStatus.AVAILABLE);
        value.setIsActive(true);
        return value;
    }

    private Driver driver() {
        Driver value = new Driver();
        value.setId(601L);
        value.setUser(driverUser);
        value.setFullName("Driver A");
        value.setLicenseNumber("DRV-001");
        value.setLicenseExpiry(LocalDate.of(2029, 12, 31));
        value.setStatus(DriverStatus.AVAILABLE);
        value.setIsActive(true);
        return value;
    }

    private <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    private final class TransferRepoHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findWithDetailsById" -> Optional.of(transfer);
                case "save" -> args[0];
                case "existsByTransferNumber" -> false;
                case "existsByExternalInstructionCodeAndSourceWarehouseIdAndDestinationWarehouseIdAndDocumentDateAndStatusNotIn" -> false;
                case "existsByExternalInstructionCodeAndSourceWarehouseIdAndDestinationWarehouseIdAndDocumentDateAndStatusNotInAndIdNot" -> false;
                case "findAllByOrderByCreatedAtDesc" -> List.of(transfer);
                default -> defaultValue(method.getReturnType());
            };
        }
    }

    private final class TransferItemRepoHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findByTransferIdOrderById" -> List.copyOf(transferItems);
                case "save" -> {
                    TransferItem item = (TransferItem) args[0];
                    if (item.getId() == null) {
                        item.setId(transferItem.getId());
                    }
                    transferItems.clear();
                    transferItems.add(item);
                    transferItem = item;
                    yield item;
                }
                case "deleteByTransferId" -> {
                    transferItems.clear();
                    yield null;
                }
                default -> defaultValue(method.getReturnType());
            };
        }
    }

    private final class InventoryRepoHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findReservableForUpdate" -> List.of(sourceInventory);
                case "findByIdForUpdate" -> Optional.of(findInventoryById((Long) args[0]));
                case "findByStockKeyForUpdate" -> Optional.ofNullable(findInventoryByKey((Long) args[0], (Long) args[1], (Long) args[2], (Long) args[3]));
                case "save" -> saveInventory((Inventory) args[0]);
                default -> defaultValue(method.getReturnType());
            };
        }

        private Inventory findInventoryById(Long id) {
            if (sourceInventory != null && sourceInventory.getId().equals(id)) return sourceInventory;
            if (transitInventory != null && transitInventory.getId().equals(id)) return transitInventory;
            if (destinationInventory != null && destinationInventory.getId().equals(id)) return destinationInventory;
            if (quarantineInventory != null && quarantineInventory.getId().equals(id)) return quarantineInventory;
            throw new ResourceNotFoundException("Inventory not found");
        }

        private Inventory findInventoryByKey(Long warehouseId, Long productId, Long batchId, Long locationId) {
            if (matches(sourceInventory, warehouseId, productId, batchId, locationId)) return sourceInventory;
            if (matches(transitInventory, warehouseId, productId, batchId, locationId)) return transitInventory;
            if (matches(destinationInventory, warehouseId, productId, batchId, locationId)) return destinationInventory;
            if (matches(quarantineInventory, warehouseId, productId, batchId, locationId)) return quarantineInventory;
            return null;
        }

        private boolean matches(Inventory inventory, Long warehouseId, Long productId, Long batchId, Long locationId) {
            return inventory != null
                    && inventory.getWarehouse().getId().equals(warehouseId)
                    && inventory.getProduct().getId().equals(productId)
                    && inventory.getBatch().getId().equals(batchId)
                    && inventory.getLocation().getId().equals(locationId);
        }

        private Inventory saveInventory(Inventory value) {
            if (value.getWarehouse().getId().equals(sourceWarehouse.getId())) {
                sourceInventory = value;
            } else if (value.getWarehouse().getId().equals(transitWarehouse.getId())) {
                transitInventory = value;
            } else if (value.getLocation().getIsQuarantine()) {
                quarantineInventory = value;
            } else {
                destinationInventory = value;
            }
            if (value.getId() == null) {
                value.setId(900L);
            }
            return value;
        }
    }

    private final class WarehouseRepoHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("findByCode".equals(method.getName())) {
                return Optional.of(transitWarehouse);
            }
            return defaultValue(method.getReturnType());
        }
    }

    private final class LocationRepoHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findByWarehouseIdAndTypeAndIsActiveTrue" -> List.of(transitLocation);
                case "findByWarehouseIdAndIsQuarantineTrueAndIsActiveTrue" -> List.of(quarantineLocation);
                case "findById" -> {
                    Long id = (Long) args[0];
                    if (destinationLocation.getId().equals(id)) yield Optional.of(destinationLocation);
                    if (quarantineLocation.getId().equals(id)) yield Optional.of(quarantineLocation);
                    if (transitLocation.getId().equals(id)) yield Optional.of(transitLocation);
                    if (sourceLocation.getId().equals(id)) yield Optional.of(sourceLocation);
                    yield Optional.empty();
                }
                default -> defaultValue(method.getReturnType());
            };
        }
    }

    private final class AssignmentRepoHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("findWarehouseIdsByUserId".equals(method.getName())) {
                return assignments.getOrDefault((Long) args[0], List.of());
            }
            return defaultValue(method.getReturnType());
        }
    }

    private final class VehicleRepoHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("findById".equals(method.getName())) {
                return Optional.of(vehicle);
            }
            return defaultValue(method.getReturnType());
        }
    }

    private final class DriverRepoHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("findById".equals(method.getName())) {
                return Optional.of(driver);
            }
            return defaultValue(method.getReturnType());
        }
    }

    private final class TripRepoHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "existsByTripNumber" -> false;
                case "existsByVehicleIdAndPlannedDateAndStatusIn", "existsByDriverIdAndPlannedDateAndStatusIn" -> false;
                case "save" -> {
                    transferTrip = (Trip) args[0];
                    if (transferTrip.getId() == null) {
                        transferTrip.setId(701L);
                    }
                    yield transferTrip;
                }
                default -> defaultValue(method.getReturnType());
            };
        }
    }

    private final class EntityManagerHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("getReference".equals(method.getName()) && args.length == 2) {
                Class<?> type = (Class<?>) args[0];
                Long id = (Long) args[1];
                if (type == Warehouse.class) {
                    if (sourceWarehouse.getId().equals(id)) return sourceWarehouse;
                    if (destinationWarehouse.getId().equals(id)) return destinationWarehouse;
                    if (transitWarehouse.getId().equals(id)) return transitWarehouse;
                }
                if (type == Product.class) {
                    return product;
                }
                if (type == WarehouseLocation.class) {
                    if (sourceLocation.getId().equals(id)) return sourceLocation;
                    if (destinationLocation.getId().equals(id)) return destinationLocation;
                    if (transitLocation.getId().equals(id)) return transitLocation;
                    if (quarantineLocation.getId().equals(id)) return quarantineLocation;
                }
            }
            return defaultValue(method.getReturnType());
        }
    }

    private final class TrackingAuditUtil extends PartnerAuditUtil {
        private AuditAction lastAction;

        TrackingAuditUtil() {
            super(null);
        }

        @Override
        public void logChange(User actor, AuditAction action, String entityType, Long entityId, String entityCode,
                              Map<String, Object> before, Map<String, Object> after) {
            lastAction = action;
        }
    }

    private final class TrackingAllocationRepository implements InvocationHandler {
        private final List<TransferAllocation> saved = new ArrayList<>();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "save" -> {
                    saved.add((TransferAllocation) args[0]);
                    yield args[0];
                }
                case "findByTransferItemTransferId", "findByTransferItemId" -> saved;
                case "deleteByTransferItemTransferId" -> {
                    saved.clear();
                    yield null;
                }
                default -> defaultValue(method.getReturnType());
            };
        }
    }

    private Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class || type == long.class || type == short.class || type == byte.class) {
            return 0;
        }
        if (type == double.class || type == float.class) {
            return 0.0;
        }
        if (type == void.class) {
            return null;
        }
        return null;
    }
}
