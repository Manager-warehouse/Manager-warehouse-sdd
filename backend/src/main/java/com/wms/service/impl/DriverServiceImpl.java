package com.wms.service.impl;

import com.wms.dto.request.DriverRequest;
import com.wms.dto.response.DriverResponse;
import com.wms.dto.response.UserResponse;
import com.wms.entity.Driver;
import com.wms.entity.User;
import com.wms.entity.Warehouse;
import com.wms.enums.AuditAction;
import com.wms.enums.DriverStatus;
import com.wms.enums.UserRole;
import com.wms.exception.ResourceNotFoundException;
import com.wms.mapper.MasterDataMapper;
import com.wms.repository.DriverRepository;
import com.wms.repository.UserRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.service.AuditLogService;
import com.wms.service.DriverService;
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

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final UserWarehouseAssignmentRepository assignmentRepository;
    private final WarehouseRepository warehouseRepository;
    private final MasterDataMapper mapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getDriverUserCandidates(Long actorId) {
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
        ensureUserWithinActorScope(actor, driverUser);

        if (driverRepository.existsByUserId(request.getUserId())) {
            throw new IllegalArgumentException("DUPLICATE_DRIVER_USER");
        }

        String phone = request.getPhone();
        if (phone == null || phone.trim().isEmpty()) {
            phone = driverUser.getPhone();
        }

        Driver driver = new Driver();
        driver.setWarehouse(warehouse);
        driver.setUser(driverUser);
        driver.setFullName(request.getFullName());
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

        // Audit Log
        auditLogService.log(actor, AuditAction.CREATE, "Driver", saved.getId(), saved.getLicenseNumber(), null, null,
                toMap(saved));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public DriverResponse updateDriver(Long id, DriverRequest request, Long userId) {
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
        driver.setFullName(request.getFullName());
        driver.setPhone(phone);
        driver.setLicenseNumber(request.getLicenseNumber());
        driver.setLicenseExpiry(request.getLicenseExpiry());
        driver.setUpdatedBy(actor);
        driver.setUpdatedAt(OffsetDateTime.now());

        Driver saved = driverRepository.save(driver);

        // Audit Log
        auditLogService.log(actor, AuditAction.UPDATE, "Driver", saved.getId(), saved.getLicenseNumber(), null, oldMap,
                toMap(saved));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public DriverResponse updateStatus(Long id, String status, Long userId) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        ensureDriverWithinActorScope(actor, driver);

        Map<String, Object> oldMap = toMap(driver);

        driver.setStatus(DriverStatus.valueOf(status));
        driver.setUpdatedBy(actor);
        driver.setUpdatedAt(OffsetDateTime.now());

        Driver saved = driverRepository.save(driver);

        // Audit Log
        auditLogService.log(actor, AuditAction.STATUS_CHANGE, "Driver", saved.getId(), saved.getLicenseNumber(), null,
                oldMap, toMap(saved));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deactivateDriver(Long id, Long userId) {
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
        ensureDriverWithinActorScope(actor, driver);

        Map<String, Object> oldMap = toMap(driver);

        driver.setIsActive(false);
        driver.setUpdatedBy(actor);
        driver.setUpdatedAt(OffsetDateTime.now());

        Driver saved = driverRepository.save(driver);

        // Audit Log
        auditLogService.log(actor, AuditAction.SOFT_DELETE, "Driver", saved.getId(), saved.getLicenseNumber(), null,
                oldMap, toMap(saved));
    }

    @Override
    @Transactional
    public DriverResponse reactivateDriver(Long id, Long userId) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        ensureDriverWithinActorScope(actor, driver);

        if (driver.getIsActive()) {
            return toResponse(driver);
        }

        Map<String, Object> oldMap = toMap(driver);

        driver.setIsActive(true);
        driver.setUpdatedBy(actor);
        driver.setUpdatedAt(OffsetDateTime.now());

        Driver saved = driverRepository.save(driver);

        // Audit Log
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

    private boolean isWithinActorScope(User actor, List<Long> actorWarehouseIds, List<Long> targetWarehouseIds) {
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
