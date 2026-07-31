package com.wms.service.driver_management.impl;


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
import com.wms.dto.request.driver_management.DriverRequest;
import com.wms.dto.response.driver_management.DriverResponse;
import com.wms.dto.response.UserResponse;
import com.wms.entity.driver_management.Driver;
import com.wms.entity.access_control.User;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.driver_management.DriverStatus;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.ResourceNotFoundException;
import com.wms.mapper.MasterDataMapper;
import com.wms.repository.driver_management.DriverRepository;
import com.wms.repository.UserRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.service.audit_trail.AuditLogService;
import com.wms.service.driver_management.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    /*
     * Service quản lý hồ sơ tài xế nội bộ.
     * Điều phối viên là người bật/tắt hồ sơ tài xế; hệ thống tự chuyển tài xế sang "đang chạy chuyến"
     * khi chuyến bắt đầu và trả về "sẵn sàng" khi chuyến kết thúc.
     */
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final UserWarehouseAssignmentRepository assignmentRepository;
    private final WarehouseRepository warehouseRepository;
    private final MasterDataMapper mapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getDriverUserCandidates(Long actorId) {
        // Lấy danh sách tài khoản có vai trò tài xế để liên kết vào hồ sơ tài xế.
        // Người xem chỉ thấy tài khoản nằm trong kho mình phụ trách, còn Admin/CEO thấy toàn bộ.
        User actor = requireUser(actorId);
        List<Long> actorWarehouseIds = getActorWarehouseIds(actor);

        return userRepository.findByRole(UserRole.DRIVER).stream()
                .filter(user -> isWithinActorScope(actor, actorWarehouseIds, getWarehouseIds(user)))
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverResponse> getAllDrivers(String status, Boolean isActive, Long actorId) {
        // Danh sách tài xế được lọc theo trạng thái làm việc, trạng thái bật/tắt và phạm vi kho của người xem.
        User actor = requireUser(actorId);
        List<Long> actorWarehouseIds = getActorWarehouseIds(actor);

        List<Driver> list = driverRepository.findAll().stream()
                .filter(d -> status == null || d.getStatus().name().equals(status))
                .filter(d -> isActive == null || d.getIsActive().equals(isActive))
                .filter(d -> isWithinActorScope(actor, actorWarehouseIds, getWarehouseIds(d.getUser())))
                .collect(Collectors.toList());
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DriverResponse getDriverById(Long id, Long actorId) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));
        ensureDriverWithinActorScope(requireUser(actorId), driver);
        return toResponse(driver);
    }

    @Override
    @Transactional
    public DriverResponse createDriver(DriverRequest request, Long userId) {
        // Tạo hồ sơ tài xế từ một tài khoản đã có sẵn trong hệ thống.
        // Họ tên lấy từ tài khoản để tránh nhập lệch giữa tài khoản đăng nhập và hồ sơ tài xế.
        if (driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new IllegalArgumentException("DUPLICATE_LICENSE_NUMBER");
        }

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Warehouse not found with id: " + request.getWarehouseId()));

        User driverUser = userRepository.findById(request.getUserId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Driver user not found with id: " + request.getUserId()));

        if (driverUser.getRole() != UserRole.DRIVER) {
            throw new IllegalArgumentException("USER_MUST_HAVE_DRIVER_ROLE");
        }
        // Validate: người tạo chỉ được liên kết tài khoản tài xế thuộc kho mình phụ trách.
        ensureUserWithinActorScope(actor, driverUser);

        if (driverRepository.existsByUserId(request.getUserId())) {
            throw new IllegalArgumentException("DUPLICATE_DRIVER_USER");
        }

        // Nếu biểu mẫu không nhập số điện thoại riêng cho tài xế thì dùng số điện thoại của tài khoản.
        String phone = request.getPhone();
        if (phone == null || phone.trim().isEmpty()) {
            phone = driverUser.getPhone();
        }

        Driver driver = new Driver();
        driver.setWarehouse(warehouse);
        driver.setUser(driverUser);
        driver.setFullName(driverUser.getFullName());
        driver.setPhone(phone);
        driver.setLicenseNumber(request.getLicenseNumber());
        driver.setLicenseExpiry(request.getLicenseExpiry());
        driver.setStatus(DriverStatus.AVAILABLE);
        driver.setIsActive(true);
        driver.setCreatedBy(actor);
        driver.setUpdatedBy(actor);
        driver.setCreatedAt(OffsetDateTime.now());
        driver.setUpdatedAt(OffsetDateTime.now());

        Driver saved = driverRepository.save(driver);

        // Ghi lịch sử tạo hồ sơ tài xế để truy vết ai tạo và dữ liệu ban đầu là gì.
        auditLogService.log(actor, AuditAction.CREATE, "Driver", saved.getId(), saved.getLicenseNumber(), null, null,
                toMap(saved));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public DriverResponse updateDriver(Long id, DriverRequest request, Long userId) {
        // Sửa hồ sơ tài xế: đổi kho phụ trách, tài khoản liên kết, số bằng lái/hạn bằng lái và số điện thoại.
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));

        if (!driver.getLicenseNumber().equals(request.getLicenseNumber())
                && driverRepository.existsByLicenseNumberAndIdNot(request.getLicenseNumber(), id)) {
            throw new IllegalArgumentException("DUPLICATE_LICENSE_NUMBER");
        }

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Warehouse not found with id: " + request.getWarehouseId()));

        User driverUser = userRepository.findById(request.getUserId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Driver user not found with id: " + request.getUserId()));

        if (driverUser.getRole() != UserRole.DRIVER) {
            throw new IllegalArgumentException("USER_MUST_HAVE_DRIVER_ROLE");
        }
        // Validate: người sửa phải nhìn thấy hồ sơ cũ và tài khoản tài xế mới trong phạm vi kho của mình.
        ensureDriverWithinActorScope(actor, driver);
        ensureUserWithinActorScope(actor, driverUser);

        if (!driver.getUser().getId().equals(request.getUserId())
                && driverRepository.existsByUserIdAndIdNot(request.getUserId(), id)) {
            throw new IllegalArgumentException("DUPLICATE_DRIVER_USER");
        }

        String phone = request.getPhone();
        if (phone == null || phone.trim().isEmpty()) {
            phone = driverUser.getPhone();
        }

        Map<String, Object> oldMap = toMap(driver);

        driver.setWarehouse(warehouse);
        driver.setUser(driverUser);
        driver.setFullName(driverUser.getFullName());
        driver.setPhone(phone);
        driver.setLicenseNumber(request.getLicenseNumber());
        driver.setLicenseExpiry(request.getLicenseExpiry());
        driver.setUpdatedBy(actor);
        driver.setUpdatedAt(OffsetDateTime.now());

        Driver saved = driverRepository.save(driver);

        // Ghi lịch sử trước/sau khi sửa hồ sơ tài xế.
        auditLogService.log(actor, AuditAction.UPDATE, "Driver", saved.getId(), saved.getLicenseNumber(), null, oldMap,
                toMap(saved));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public DriverResponse updateStatus(Long id, String status, Long userId) {
        // Cập nhật trạng thái làm việc do tài xế/điều phối thao tác, nhưng không cho tự chuyển sang "đang chạy chuyến".
        // Trạng thái "đang chạy chuyến" chỉ hệ thống được chuyển khi chuyến bắt đầu.
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        ensureDriverWithinActorScope(actor, driver);

        DriverStatus requestedStatus = DriverStatus.valueOf(status);
        if (requestedStatus == DriverStatus.ON_TRIP) {
            throw new IllegalArgumentException("DRIVER_ON_TRIP_STATUS_SYSTEM_MANAGED");
        }

        Map<String, Object> oldMap = toMap(driver);

        driver.setStatus(requestedStatus);
        driver.setUpdatedBy(actor);
        driver.setUpdatedAt(OffsetDateTime.now());

        Driver saved = driverRepository.save(driver);

        // Ghi lịch sử đổi trạng thái làm việc của tài xế.
        auditLogService.log(actor, AuditAction.STATUS_CHANGE, "Driver", saved.getId(), saved.getLicenseNumber(), null,
                oldMap, toMap(saved));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deactivateDriver(Long id, Long userId) {
        // Tắt hồ sơ tài xế là tắt mềm: giữ dữ liệu lịch sử, chỉ không cho dùng để gán chuyến mới.
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));

        if (!driver.getIsActive()) {
            return;
        }

        if (driver.getStatus() == DriverStatus.ON_TRIP) {
            throw new IllegalArgumentException("DRIVER_ON_TRIP");
        }

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        // Validate: chỉ điều phối viên được bật/tắt tài xế và chỉ trong kho mình phụ trách.
        ensureDispatcherActor(actor);
        ensureDriverWithinActorScope(actor, driver);

        Map<String, Object> oldMap = toMap(driver);

        driver.setIsActive(false);
        driver.setUpdatedBy(actor);
        driver.setUpdatedAt(OffsetDateTime.now());

        Driver saved = driverRepository.save(driver);

        // Ghi lịch sử tắt hồ sơ tài xế, không xóa vật lý dữ liệu.
        auditLogService.log(actor, AuditAction.SOFT_DELETE, "Driver", saved.getId(), saved.getLicenseNumber(), null,
                oldMap, toMap(saved));
    }

    @Override
    @Transactional
    public DriverResponse reactivateDriver(Long id, Long userId) {
        // Bật lại hồ sơ tài xế để có thể được gán chuyến mới.
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        ensureDispatcherActor(actor);
        ensureDriverWithinActorScope(actor, driver);

        if (driver.getIsActive()) {
            return toResponse(driver);
        }

        Map<String, Object> oldMap = toMap(driver);

        driver.setIsActive(true);
        driver.setUpdatedBy(actor);
        driver.setUpdatedAt(OffsetDateTime.now());

        Driver saved = driverRepository.save(driver);

        // Ghi lịch sử bật lại hồ sơ tài xế.
        auditLogService.log(actor, AuditAction.UPDATE, "Driver", saved.getId(), saved.getLicenseNumber(), null, oldMap,
                toMap(saved));

        return toResponse(saved);
    }

    private DriverResponse toResponse(Driver driver) {
        DriverResponse response = mapper.toResponse(driver);
        if (driver.getUser() != null) {
            response.setWarehouseIds(getWarehouseIds(driver.getUser()));
        }
        return response;
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .code(user.getCode())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .jobTitle(user.getJobTitle())
                .shift(user.getShift())
                .region(user.getRegion())
                .isActive(user.getIsActive())
                .warehouses(getWarehouseIds(user))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private List<Long> getWarehouseIds(User user) {
        if (user == null || user.getId() == null) {
            return List.of();
        }
        return assignmentRepository.findWarehouseIdsByUserId(user.getId());
    }

    private List<Long> getActorWarehouseIds(User actor) {
        return hasGlobalScope(actor) ? List.of() : getWarehouseIds(actor);
    }

    private boolean hasGlobalScope(User actor) {
        return actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.CEO;
    }

    private void ensureDispatcherActor(User actor) {
        // Quyền bật/tắt tài xế thuộc điều phối viên theo quy tắc vận hành đội xe.
        if (actor.getRole() != UserRole.DISPATCHER) {
            throw new IllegalArgumentException("DISPATCHER_ROLE_REQUIRED");
        }
    }

    private boolean isWithinActorScope(User actor, List<Long> actorWarehouseIds, List<Long> targetWarehouseIds) {
        // Admin/CEO có phạm vi toàn hệ thống; các vai trò khác phải giao nhau ít nhất một kho.
        return hasGlobalScope(actor)
                || actorWarehouseIds.stream().anyMatch(targetWarehouseIds::contains);
    }

    private void ensureUserWithinActorScope(User actor, User targetUser) {
        if (!isWithinActorScope(actor, getActorWarehouseIds(actor), getWarehouseIds(targetUser))) {
            throw new IllegalArgumentException("WAREHOUSE_SCOPE_REQUIRED");
        }
    }

    private void ensureDriverWithinActorScope(User actor, Driver driver) {
        ensureUserWithinActorScope(actor, driver.getUser());
    }

    private Map<String, Object> toMap(Driver d) {
        if (d == null)
            return null;
        Map<String, Object> map = new HashMap<>();
        map.put("id", d.getId());
        map.put("warehouseId", d.getWarehouse() != null ? d.getWarehouse().getId() : null);
        map.put("userId", d.getUser() != null ? d.getUser().getId() : null);
        map.put("fullName", d.getFullName());
        map.put("phone", d.getPhone());
        map.put("licenseNumber", d.getLicenseNumber());
        map.put("licenseExpiry", d.getLicenseExpiry() != null ? d.getLicenseExpiry().toString() : null);
        map.put("status", d.getStatus() != null ? d.getStatus().name() : null);
        map.put("isActive", d.getIsActive());
        return map;
    }
}
