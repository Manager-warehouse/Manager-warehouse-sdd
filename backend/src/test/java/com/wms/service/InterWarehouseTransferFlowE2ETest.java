package com.wms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.wms.dto.request.*;
import com.wms.dto.response.*;
import com.wms.entity.*;
import com.wms.enums.*;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.repository.*;
import com.wms.service.transfer.impl.TransferRequestServiceImpl;
import com.wms.service.transfer.impl.InterWarehouseTransferReceivingService;
import com.wms.service.transfer.impl.InterWarehouseTransferHelper;
import com.wms.mapper.InterWarehouseTransferMapper;
import com.wms.util.PartnerAuditUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterWarehouseTransferFlowE2ETest {

    // Repositories
    @Mock private TransferRequestRepository requestRepository;
    @Mock private TransferRequestItemRepository requestItemRepository;
    @Mock private InterWarehouseTransferRepository transferRepository;
    @Mock private InterWarehouseTransferItemRepository transferItemRepository;
    @Mock private InterWarehouseTransferAllocationRepository allocationRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private WarehouseLocationRepository locationRepository;
    @Mock private UserWarehouseAssignmentRepository assignmentRepository;
    @Mock private TripRepository tripRepository;
    @Mock private AdjustmentRepository adjustmentRepository;
    @Mock private QuarantineRecordRepository quarantineRecordRepository;
    @Mock private DamageReportRepository damageReportRepository;
    @Mock private ProductRepository productRepository;
    @Mock private PriceHistoryRepository priceHistoryRepository;

    // Services under test
    private TransferRequestServiceImpl requestService;
    private InterWarehouseTransferReceivingService receivingService;
    private DisposalService disposalService;

    // Helpers
    @Mock private PartnerAuditUtil auditUtil;
    @Mock private com.wms.service.transfer.InterWarehouseTransferService mockTransferService;
    @Mock private jakarta.persistence.EntityManager entityManager;
    @Mock private AuditLogService auditLogService;
    @Mock private ReceiptValidationService receiptValidationService;
    @Mock private ReceiptItemRepository receiptItemRepository;

    // Mock Entities
    private User manager; // acts as destination manager
    private User sourceManager;
    private User ceo;
    private User planner;
    private User storekeeper;

    private Warehouse sourceWarehouse;
    private Warehouse destinationWarehouse;
    private WarehouseLocation sourceLocation;
    private WarehouseLocation destinationLocation;
    private WarehouseLocation quarantineLocation;

    private Product product;
    private Batch batch;
    private TransferRequest transferRequest;
    private InterWarehouseTransfer transfer;
    private InterWarehouseTransferItem transferItem;

    @BeforeEach
    void setUp() {
        // Users & Roles
        manager = new User();
        manager.setId(10L);
        manager.setRole(UserRole.WAREHOUSE_MANAGER);
        manager.setFullName("Destination Manager Name");

        sourceManager = new User();
        sourceManager.setId(11L);
        sourceManager.setRole(UserRole.WAREHOUSE_MANAGER);
        sourceManager.setFullName("Source Manager Name");

        ceo = new User();
        ceo.setId(20L);
        ceo.setRole(UserRole.CEO);
        ceo.setFullName("CEO Name");

        planner = new User();
        planner.setId(30L);
        planner.setRole(UserRole.PLANNER);
        planner.setFullName("Planner Name");

        storekeeper = new User();
        storekeeper.setId(40L);
        storekeeper.setRole(UserRole.STOREKEEPER);
        storekeeper.setFullName("Storekeeper Name");

        // Warehouses & Locations
        sourceWarehouse = new Warehouse();
        sourceWarehouse.setId(1L);
        sourceWarehouse.setCode("HP-01");
        sourceWarehouse.setName("Hai Phong");

        destinationWarehouse = new Warehouse();
        destinationWarehouse.setId(2L);
        destinationWarehouse.setCode("HN-01");
        destinationWarehouse.setName("Ha Noi");

        sourceLocation = new WarehouseLocation();
        sourceLocation.setId(101L);
        sourceLocation.setCode("BIN-SRC");
        sourceLocation.setIsQuarantine(false);
        sourceLocation.setWarehouse(sourceWarehouse);

        destinationLocation = new WarehouseLocation();
        destinationLocation.setId(102L);
        destinationLocation.setCode("BIN-DEST");
        destinationLocation.setIsQuarantine(false);
        destinationLocation.setWarehouse(destinationWarehouse);

        quarantineLocation = new WarehouseLocation();
        quarantineLocation.setId(103L);
        quarantineLocation.setCode("BIN-QUAR");
        quarantineLocation.setIsQuarantine(true);
        quarantineLocation.setWarehouse(destinationWarehouse);

        // Virtual Transit Warehouse & Location
        Warehouse transitWarehouse = new Warehouse();
        transitWarehouse.setId(999L);
        transitWarehouse.setCode("IN_TRANSIT");
        transitWarehouse.setName("In Transit Warehouse");

        WarehouseLocation transitLocation = new WarehouseLocation();
        transitLocation.setId(9991L);
        transitLocation.setCode("BIN-TRANSIT");
        transitLocation.setWarehouse(transitWarehouse);

        // Product
        product = new Product();
        product.setId(500L);
        product.setSku("PAN-001");
        product.setName("Chảo chống dính");
        product.setUnit("cái");

        // Entities
        transferRequest = new TransferRequest();
        transferRequest.setId(700L);
        transferRequest.setRequestNumber("TRQ-20260628-0001");
        transferRequest.setSourceWarehouse(sourceWarehouse);
        transferRequest.setDestinationWarehouse(destinationWarehouse);
        transferRequest.setStatus(TransferRequestStatus.DRAFT);
        transferRequest.setCreatedBy(manager);

        TransferRequestItem reqItem = new TransferRequestItem();
        reqItem.setId(701L);
        reqItem.setTransferRequest(transferRequest);
        reqItem.setProduct(product);
        reqItem.setRequestedQty(new BigDecimal("30.00"));
        transferRequest.setItems(new ArrayList<>(List.of(reqItem)));

        transfer = new InterWarehouseTransfer();
        transfer.setId(800L);
        transfer.setTransferNumber("TRF-20260628-0001");
        transfer.setSourceWarehouse(sourceWarehouse);
        transfer.setDestinationWarehouse(destinationWarehouse);
        transfer.setStatus(InterWarehouseTransferStatus.NEW);

        transferItem = new InterWarehouseTransferItem();
        transferItem.setId(801L);
        transferItem.setTransfer(transfer);
        transferItem.setProduct(product);
        transferItem.setSourceLocation(sourceLocation);
        transferItem.setDestinationLocation(destinationLocation);
        transferItem.setPlannedQty(new BigDecimal("30.00"));
        transferItem.setSentQty(new BigDecimal("30.00"));
        transfer.setItems(new ArrayList<>(List.of(transferItem)));

        // Create Batch and Allocation link to prevent empty allocations list
        batch = new Batch();
        batch.setId(66L);
        batch.setProduct(product);

        Inventory srcInventory = new Inventory();
        srcInventory.setWarehouse(sourceWarehouse);
        srcInventory.setProduct(product);
        srcInventory.setLocation(sourceLocation);
        srcInventory.setBatch(batch);
        srcInventory.setTotalQty(new BigDecimal("30.00"));
        srcInventory.setReservedQty(new BigDecimal("30.00"));

        InterWarehouseTransferAllocation allocation = new InterWarehouseTransferAllocation();
        allocation.setId(850L);
        allocation.setTransferItem(transferItem);
        allocation.setInventory(srcInventory);
        allocation.setAllocatedQty(new BigDecimal("30.00"));

        // Attach Trip & Driver & Vehicle & Driver User to bypass finalReceive NPE
        User driverUser = new User();
        driverUser.setId(99L);
        Driver driver = new Driver();
        driver.setId(95L);
        driver.setUser(driverUser);
        Vehicle vehicle = new Vehicle();
        vehicle.setId(88L);
        vehicle.setPlateNumber("29C-12345");
        Trip trip = new Trip();
        trip.setId(90L);
        trip.setDriver(driver);
        trip.setVehicle(vehicle);
        transfer.setTrip(trip);

        // Initialize Services
        requestService = new TransferRequestServiceImpl(
                requestRepository,
                requestItemRepository,
                warehouseRepository,
                productRepository,
                inventoryRepository,
                assignmentRepository,
                mockTransferService,
                auditUtil
        );

        InterWarehouseTransferMapper mapper = new InterWarehouseTransferMapper();
        InterWarehouseTransferHelper helper = new InterWarehouseTransferHelper(
                transferRepository,
                transferItemRepository,
                allocationRepository,
                inventoryRepository,
                locationRepository,
                assignmentRepository,
                tripRepository,
                mapper,
                auditUtil,
                entityManager
        );

        receivingService = new InterWarehouseTransferReceivingService(
                transferRepository,
                transferItemRepository,
                allocationRepository,
                inventoryRepository,
                warehouseRepository,
                locationRepository,
                adjustmentRepository,
                auditUtil,
                helper,
                quarantineRecordRepository
        );

        disposalService = new DisposalService(
                receiptItemRepository,
                damageReportRepository,
                adjustmentRepository,
                inventoryRepository,
                locationRepository,
                priceHistoryRepository,
                assignmentRepository,
                receiptValidationService,
                auditLogService,
                quarantineRecordRepository
        );

        // Lenient stubs for saving and finding items to avoid NullPointerExceptions
        lenient().when(transferRepository.save(any(InterWarehouseTransfer.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(transferItemRepository.save(any(InterWarehouseTransferItem.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(transferItemRepository.findByTransferIdOrderById(anyLong())).thenAnswer(i -> List.of(transferItem));
        lenient().when(warehouseRepository.findByCode("IN_TRANSIT")).thenReturn(Optional.of(transitWarehouse));
        lenient().when(locationRepository.findByWarehouseIdAndTypeAndIsActiveTrue(eq(transitWarehouse.getId()), eq(LocationType.BIN)))
                .thenReturn(List.of(transitLocation));
        lenient().when(locationRepository.findByWarehouseIdAndIsQuarantineTrueAndIsActiveTrue(anyLong()))
                .thenReturn(List.of(quarantineLocation));
        lenient().when(allocationRepository.findByTransferItemId(anyLong())).thenAnswer(i -> List.of(allocation));
        lenient().when(entityManager.getReference(eq(WarehouseLocation.class), eq(destinationLocation.getId())))
                .thenReturn(destinationLocation);
        lenient().when(entityManager.getReference(eq(WarehouseLocation.class), eq(quarantineLocation.getId())))
                .thenReturn(quarantineLocation);
    }

    @Test
    void testE2ETransferFlow_happyPath() {
        // --- 1. Manager requests transfer (DRAFT) ---
        when(assignmentRepository.findWarehouseIdsByUserId(manager.getId())).thenReturn(List.of(destinationWarehouse.getId()));
        when(warehouseRepository.findById(sourceWarehouse.getId())).thenReturn(Optional.of(sourceWarehouse));
        when(warehouseRepository.findById(destinationWarehouse.getId())).thenReturn(Optional.of(destinationWarehouse));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(requestRepository.save(any(TransferRequest.class))).thenReturn(transferRequest);

        TransferRequestCreateRequest createReq = new TransferRequestCreateRequest(
                sourceWarehouse.getId(),
                destinationWarehouse.getId(),
                "Cần chảo gấp cho Hà Nội",
                List.of(new TransferRequestItemRequest(product.getId(), new BigDecimal("30.00")))
        );
        TransferRequestResponse draftResponse = requestService.createRequest(createReq, manager);
        assertThat(draftResponse.status()).isEqualTo(TransferRequestStatus.DRAFT);

        // --- 2. Manager submits for CEO approval (SUBMITTED) ---
        when(requestRepository.findById(transferRequest.getId())).thenReturn(Optional.of(transferRequest));
        TransferRequestResponse submittedResponse = requestService.submitRequest(transferRequest.getId(), manager);
        assertThat(submittedResponse.status()).isEqualTo(submittedResponse.status());

        // --- 3. CEO Approves (APPROVED) ---
        TransferRequestResponse approvedResponse = requestService.approveRequest(transferRequest.getId(), ceo);
        assertThat(approvedResponse.status()).isEqualTo(TransferRequestStatus.APPROVED);

        // --- 4. Planner converts to actual TRF (CONVERTED) ---
        InterWarehouseTransferResponse mockTrfRes = new InterWarehouseTransferResponse(
                800L, "TRF-20260628-0001", "TRQ-20260628-0001",
                sourceWarehouse.getId(), sourceWarehouse.getCode(),
                destinationWarehouse.getId(), destinationWarehouse.getCode(),
                InterWarehouseTransferStatus.NEW, null, null, null, null, null, null, null,
                LocalDate.now(), LocalDate.now(), null, null, false, false, null, null, null,
                null, "Cần chảo gấp cho Hà Nội", false, OffsetDateTime.now(), OffsetDateTime.now(), List.of()
        );
        when(mockTransferService.createTransfer(any(InterWarehouseTransferCreateRequest.class), eq(planner)))
                .thenReturn(mockTrfRes);
        TransferRequestResponse convertedResponse = requestService.convertToTransfer(transferRequest.getId(), planner);
        assertThat(convertedResponse.status()).isEqualTo(TransferRequestStatus.CONVERTED);

        // --- 5. Destination Storekeeper counts items (IN_TRANSIT -> receive count) ---
        transfer.setStatus(InterWarehouseTransferStatus.IN_TRANSIT);
        when(transferRepository.findWithDetailsById(transfer.getId())).thenReturn(Optional.of(transfer));
        when(assignmentRepository.findWarehouseIdsByUserId(storekeeper.getId())).thenReturn(List.of(destinationWarehouse.getId()));

        List<InterWarehouseTransferReceiveCountItemRequest> countItems = List.of(
                new InterWarehouseTransferReceiveCountItemRequest(transferItem.getId(), new BigDecimal("30.00"), "")
        );
        InterWarehouseTransferResponse countResponse = receivingService.receiveCount(
                transfer.getId(),
                new InterWarehouseTransferReceiveCountRequest(countItems),
                storekeeper
        );
        assertThat(countResponse.status()).isEqualTo(InterWarehouseTransferStatus.IN_TRANSIT);
        assertThat(transferItem.getWorkerReceivedQty()).isEqualByComparingTo("30.00");

        // --- 6. Destination QC checks items (BIN check) ---
        when(locationRepository.findById(destinationLocation.getId())).thenReturn(Optional.of(destinationLocation));
        List<InterWarehouseTransferReceiveCheckItemRequest> checkItems = List.of(
                new InterWarehouseTransferReceiveCheckItemRequest(
                        transferItem.getId(), new BigDecimal("30.00"), new BigDecimal("30.00"),
                        BigDecimal.ZERO, destinationLocation.getId(), "", ""
                )
        );
        InterWarehouseTransferResponse checkResponse = receivingService.receiveCheck(
                transfer.getId(),
                new InterWarehouseTransferReceiveCheckRequest(checkItems),
                storekeeper
        );
        assertThat(checkResponse.status()).isEqualTo(InterWarehouseTransferStatus.IN_TRANSIT);
        assertThat(transferItem.getReceivedQty()).isEqualByComparingTo("30.00");
        assertThat(transferItem.getQcPassedQty()).isEqualByComparingTo("30.00");
        assertThat(transferItem.getQcFailedQty()).isEqualByComparingTo("0.00");

        // --- 7. Manager finalizes receipt (Happy path -> COMPLETED) ---
        when(assignmentRepository.findWarehouseIdsByUserId(manager.getId())).thenReturn(List.of(destinationWarehouse.getId()));
        
        Inventory destInventory = new Inventory();
        destInventory.setWarehouse(destinationWarehouse);
        destInventory.setProduct(product);
        destInventory.setLocation(destinationLocation);
        destInventory.setBatch(batch);
        destInventory.setTotalQty(BigDecimal.ZERO);
        destInventory.setReservedQty(BigDecimal.ZERO);

        Inventory transitInventory = new Inventory();
        transitInventory.setWarehouse(sourceWarehouse); // virtual transit
        transitInventory.setProduct(product);
        transitInventory.setLocation(destinationLocation); // not strictly checked in test, just need ID
        transitInventory.setBatch(batch);
        transitInventory.setTotalQty(new BigDecimal("30.00"));
        transitInventory.setReservedQty(BigDecimal.ZERO);

        when(inventoryRepository.findByStockKeyForUpdate(anyLong(), anyLong(), anyLong(), eq(9991L)))
                .thenReturn(Optional.of(transitInventory));
        when(inventoryRepository.findByStockKeyForUpdate(anyLong(), anyLong(), anyLong(), eq(destinationLocation.getId())))
                .thenReturn(Optional.of(destInventory));

        InterWarehouseTransferResponse finalResponse = receivingService.finalReceive(
                transfer.getId(),
                new InterWarehouseTransferFinalReceiveRequest("Nhận đủ hàng chảo"),
                manager
        );
        assertThat(finalResponse.status()).isEqualTo(InterWarehouseTransferStatus.COMPLETED);
        assertThat(destInventory.getTotalQty()).isEqualByComparingTo("30.00");
    }

    @Test
    void testE2ETransferFlow_shortageDiscrepancy() {
        // --- Setup counts showing shortage: Sent 30, received 28 ---
        transfer.setStatus(InterWarehouseTransferStatus.IN_TRANSIT);
        when(transferRepository.findWithDetailsById(transfer.getId())).thenReturn(Optional.of(transfer));
        when(assignmentRepository.findWarehouseIdsByUserId(storekeeper.getId())).thenReturn(List.of(destinationWarehouse.getId()));

        List<InterWarehouseTransferReceiveCountItemRequest> countItems = List.of(
                new InterWarehouseTransferReceiveCountItemRequest(transferItem.getId(), new BigDecimal("28.00"), "Mất mát dọc đường")
        );
        receivingService.receiveCount(transfer.getId(), new InterWarehouseTransferReceiveCountRequest(countItems), storekeeper);

        when(locationRepository.findById(destinationLocation.getId())).thenReturn(Optional.of(destinationLocation));
        List<InterWarehouseTransferReceiveCheckItemRequest> checkItems = List.of(
                new InterWarehouseTransferReceiveCheckItemRequest(
                        transferItem.getId(), new BigDecimal("28.00"), new BigDecimal("28.00"),
                        BigDecimal.ZERO, destinationLocation.getId(), "", ""
                )
        );
        receivingService.receiveCheck(transfer.getId(), new InterWarehouseTransferReceiveCheckRequest(checkItems), storekeeper);

        // --- Final Receive with Shortage -> Status is COMPLETED_WITH_DISCREPANCY ---
        when(assignmentRepository.findWarehouseIdsByUserId(manager.getId())).thenReturn(List.of(destinationWarehouse.getId()));
        
        Inventory destInventory = new Inventory();
        destInventory.setWarehouse(destinationWarehouse);
        destInventory.setProduct(product);
        destInventory.setLocation(destinationLocation);
        destInventory.setBatch(batch);
        destInventory.setTotalQty(BigDecimal.ZERO);
        destInventory.setReservedQty(BigDecimal.ZERO);

        Inventory transitInventory = new Inventory();
        transitInventory.setWarehouse(sourceWarehouse);
        transitInventory.setProduct(product);
        transitInventory.setLocation(destinationLocation);
        transitInventory.setBatch(batch);
        transitInventory.setTotalQty(new BigDecimal("30.00"));
        transitInventory.setReservedQty(BigDecimal.ZERO);

        when(inventoryRepository.findByStockKeyForUpdate(anyLong(), anyLong(), anyLong(), eq(9991L)))
                .thenReturn(Optional.of(transitInventory));
        when(inventoryRepository.findByStockKeyForUpdate(anyLong(), anyLong(), anyLong(), eq(destinationLocation.getId())))
                .thenReturn(Optional.of(destInventory));

        InterWarehouseTransferResponse finalResponse = receivingService.finalReceive(
                transfer.getId(),
                new InterWarehouseTransferFinalReceiveRequest("Thiếu 2 cái chảo"),
                manager
        );

        // Asserts
        assertThat(finalResponse.status()).isEqualTo(InterWarehouseTransferStatus.COMPLETED_WITH_DISCREPANCY);
        assertThat(destInventory.getTotalQty()).isEqualByComparingTo("28.00");
        verify(adjustmentRepository, times(1)).save(argThat(adj ->
                adj.getType() == AdjustmentType.TRANSFER_DISCREPANCY
                && adj.getQuantityAdjustment().compareTo(new BigDecimal("-2.00")) == 0
        ));
    }

    @Test
    void testE2ETransferFlow_wrongSkuReturnLeg() {
        transfer.setStatus(InterWarehouseTransferStatus.IN_TRANSIT);
        when(transferRepository.findWithDetailsById(transfer.getId())).thenReturn(Optional.of(transfer));
        when(assignmentRepository.findWarehouseIdsByUserId(manager.getId())).thenReturn(List.of(destinationWarehouse.getId()));

        // 1. Destination Manager requests return to source because of wrong SKU
        InterWarehouseTransferResponse returnRequested = receivingService.requestReturn(
                transfer.getId(),
                new TransferReturnRequest("Giao sai mã SKU chảo"),
                manager
        );
        assertThat(transfer.isReturnRequested()).isTrue();
        assertThat(transfer.getReturnReason()).isEqualTo("Giao sai mã SKU chảo");

        // 2. Destination Manager approves the return trip
        InterWarehouseTransferResponse returnApproved = receivingService.approveReturn(transfer.getId(), manager);
        assertThat(transfer.isReturnRequested()).isFalse();
        assertThat(transfer.isReturned()).isTrue();
        assertThat(transfer.getStatus()).isEqualTo(InterWarehouseTransferStatus.IN_TRANSIT);
    }

    @Test
    void testE2ETransferFlow_physicalDamageToQuarantineAndDisposal() {
        transfer.setStatus(InterWarehouseTransferStatus.IN_TRANSIT);
        when(transferRepository.findWithDetailsById(transfer.getId())).thenReturn(Optional.of(transfer));
        
        // 1. Out of 30 items sent, 5 are physically damaged (failed QC) and must go to Quarantine Bin
        when(assignmentRepository.findWarehouseIdsByUserId(storekeeper.getId())).thenReturn(List.of(destinationWarehouse.getId()));
        when(locationRepository.findById(destinationLocation.getId())).thenReturn(Optional.of(destinationLocation));
        lenient().when(locationRepository.findById(quarantineLocation.getId())).thenReturn(Optional.of(quarantineLocation));

        List<InterWarehouseTransferReceiveCountItemRequest> countItems = List.of(
                new InterWarehouseTransferReceiveCountItemRequest(transferItem.getId(), new BigDecimal("30.00"), "")
        );
        receivingService.receiveCount(transfer.getId(), new InterWarehouseTransferReceiveCountRequest(countItems), storekeeper);

        List<InterWarehouseTransferReceiveCheckItemRequest> checkItems = List.of(
                new InterWarehouseTransferReceiveCheckItemRequest(
                        transferItem.getId(), new BigDecimal("30.00"), new BigDecimal("25.00"),
                        new BigDecimal("5.00"), destinationLocation.getId(), "Méo móp nặng", "Méo móp nặng"
                )
        );
        receivingService.receiveCheck(transfer.getId(), new InterWarehouseTransferReceiveCheckRequest(checkItems), storekeeper);

        // Mock QuarantineRecord save
        QuarantineRecord mockRecord = new QuarantineRecord();
        mockRecord.setId(99L);
        mockRecord.setWarehouse(destinationWarehouse);
        mockRecord.setProduct(product);
        mockRecord.setBatch(batch);
        mockRecord.setLocation(quarantineLocation);
        mockRecord.setRemainingQuantity(new BigDecimal("5.00"));
        mockRecord.setOriginType("INTERNAL_TRANSFER");
        lenient().when(quarantineRecordRepository.save(any(QuarantineRecord.class))).thenReturn(mockRecord);

        // Finalize receive
        when(assignmentRepository.findWarehouseIdsByUserId(manager.getId())).thenReturn(List.of(destinationWarehouse.getId()));
        Inventory destInventory = new Inventory();
        destInventory.setWarehouse(destinationWarehouse);
        destInventory.setProduct(product);
        destInventory.setLocation(destinationLocation);
        destInventory.setBatch(batch);
        destInventory.setTotalQty(BigDecimal.ZERO);
        destInventory.setReservedQty(BigDecimal.ZERO);
        
        Inventory quarInventory = new Inventory();
        quarInventory.setWarehouse(destinationWarehouse);
        quarInventory.setProduct(product);
        quarInventory.setLocation(quarantineLocation);
        quarInventory.setBatch(batch);
        quarInventory.setTotalQty(BigDecimal.ZERO);
        quarInventory.setReservedQty(BigDecimal.ZERO);

        Inventory transitInventory = new Inventory();
        transitInventory.setWarehouse(sourceWarehouse);
        transitInventory.setProduct(product);
        transitInventory.setLocation(destinationLocation);
        transitInventory.setBatch(batch);
        transitInventory.setTotalQty(new BigDecimal("30.00"));
        transitInventory.setReservedQty(BigDecimal.ZERO);

        when(inventoryRepository.findByStockKeyForUpdate(anyLong(), anyLong(), anyLong(), eq(9991L)))
                .thenReturn(Optional.of(transitInventory));
        when(inventoryRepository.findByStockKeyForUpdate(anyLong(), anyLong(), anyLong(), eq(destinationLocation.getId())))
                .thenReturn(Optional.of(destInventory));
        when(inventoryRepository.findByStockKeyForUpdate(anyLong(), anyLong(), anyLong(), eq(quarantineLocation.getId())))
                .thenReturn(Optional.of(quarInventory));

        receivingService.finalReceive(transfer.getId(), new InterWarehouseTransferFinalReceiveRequest("Nhận hàng lỗi"), manager);

        // Asserts: QC passed items added to standard stock, QC failed items added to quarantine stock
        assertThat(destInventory.getTotalQty()).isEqualByComparingTo("25.00");
        assertThat(quarInventory.getTotalQty()).isEqualByComparingTo("5.00");
        verify(quarantineRecordRepository, times(1)).save(argThat(qr ->
                qr.getRemainingQuantity().compareTo(new BigDecimal("5.00")) == 0
                && qr.getOriginType().equals("INTERNAL_TRANSFER")
        ));

        // 2. Try to dispose of the damaged items in Quarantine Bin
        DisposalRequest disposalReq = new DisposalRequest("Tiêu hủy 5 chảo hỏng do chuyển kho", "http://img");
        
        when(quarantineRecordRepository.findById(99L)).thenReturn(Optional.of(mockRecord));
        when(inventoryRepository.findByWarehouseProductBatchLocationForUpdate(anyLong(), anyLong(), anyLong(), eq(quarantineLocation.getId())))
                .thenReturn(Optional.of(quarInventory));
        when(adjustmentRepository.existsByReferenceTypeAndReferenceIdAndType(any(), any(), any())).thenReturn(false);
        when(damageReportRepository.save(any(DamageReport.class))).thenAnswer(i -> {
            DamageReport dr = i.getArgument(0);
            dr.setId(991L);
            return dr;
        });
        when(adjustmentRepository.save(any(Adjustment.class))).thenAnswer(i -> {
            Adjustment adj = i.getArgument(0);
            adj.setId(777L);
            return adj;
        });

        DisposalResponse disposalResponse = disposalService.createDisposalFromQuarantine(99L, disposalReq, manager);
        
        // Assert: Auto-approved as value of 5 items is low (5 * 0 = 0 VND < 5M)
        assertThat(disposalResponse.isAutoApproved()).isTrue();
        assertThat(mockRecord.getRemainingQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
