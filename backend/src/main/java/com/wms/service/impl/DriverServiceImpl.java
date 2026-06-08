package com.wms.service.impl;

import com.wms.dto.request.DriverRequest;
import com.wms.dto.response.DriverResponse;
import com.wms.entity.Driver;
import com.wms.entity.User;
import com.wms.enums.AuditAction;
import com.wms.enums.DriverStatus;
import com.wms.enums.UserRole;
import com.wms.exception.ResourceNotFoundException;
import com.wms.mapper.MasterDataMapper;
import com.wms.repository.DriverRepository;
import com.wms.repository.UserRepository;
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
    private final MasterDataMapper mapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<DriverResponse> getAllDrivers(String status, Boolean isActive) {
        List<Driver> list = driverRepository.findAll().stream()
                .filter(d -> status == null || d.getStatus().name().equals(status))
                .filter(d -> isActive == null || d.getIsActive().equals(isActive))
                .collect(Collectors.toList());
        return list.stream().map(mapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DriverResponse getDriverById(Long id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));
        return mapper.toResponse(driver);
    }

    @Override
    @Transactional
    public DriverResponse createDriver(DriverRequest request, Long userId) {
        if (driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new IllegalArgumentException("DUPLICATE_LICENSE_NUMBER");
        }

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        User driverUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver user not found with id: " + request.getUserId()));

        if (driverUser.getRole() != UserRole.DRIVER) {
            throw new IllegalArgumentException("USER_MUST_HAVE_DRIVER_ROLE");
        }

        if (driverRepository.existsByUserId(request.getUserId())) {
            throw new IllegalArgumentException("DUPLICATE_DRIVER_USER");
        }

        String phone = request.getPhone();
        if (phone == null || phone.trim().isEmpty()) {
            phone = driverUser.getPhone();
        }

        Driver driver = new Driver();
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
        auditLogService.log(actor, AuditAction.CREATE, "Driver", saved.getId(), saved.getLicenseNumber(), null, null, toMap(saved));

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DriverResponse updateDriver(Long id, DriverRequest request, Long userId) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));

        if (!driver.getLicenseNumber().equals(request.getLicenseNumber()) && driverRepository.existsByLicenseNumberAndIdNot(request.getLicenseNumber(), id)) {
            throw new IllegalArgumentException("DUPLICATE_LICENSE_NUMBER");
        }

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        User driverUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver user not found with id: " + request.getUserId()));

        if (driverUser.getRole() != UserRole.DRIVER) {
            throw new IllegalArgumentException("USER_MUST_HAVE_DRIVER_ROLE");
        }

        if (!driver.getUser().getId().equals(request.getUserId()) && driverRepository.existsByUserIdAndIdNot(request.getUserId(), id)) {
            throw new IllegalArgumentException("DUPLICATE_DRIVER_USER");
        }

        String phone = request.getPhone();
        if (phone == null || phone.trim().isEmpty()) {
            phone = driverUser.getPhone();
        }

        Map<String, Object> oldMap = toMap(driver);

        driver.setUser(driverUser);
        driver.setFullName(request.getFullName());
        driver.setPhone(phone);
        driver.setLicenseNumber(request.getLicenseNumber());
        driver.setLicenseExpiry(request.getLicenseExpiry());
        driver.setUpdatedBy(actor);
        driver.setUpdatedAt(OffsetDateTime.now());

        Driver saved = driverRepository.save(driver);

        // Audit Log
        auditLogService.log(actor, AuditAction.UPDATE, "Driver", saved.getId(), saved.getLicenseNumber(), null, oldMap, toMap(saved));

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DriverResponse updateStatus(Long id, String status, Long userId) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Map<String, Object> oldMap = toMap(driver);

        driver.setStatus(DriverStatus.valueOf(status));
        driver.setUpdatedBy(actor);
        driver.setUpdatedAt(OffsetDateTime.now());

        Driver saved = driverRepository.save(driver);

        // Audit Log
        auditLogService.log(actor, AuditAction.STATUS_CHANGE, "Driver", saved.getId(), saved.getLicenseNumber(), null, oldMap, toMap(saved));

        return mapper.toResponse(saved);
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

        Map<String, Object> oldMap = toMap(driver);

        driver.setIsActive(false);
        driver.setUpdatedBy(actor);
        driver.setUpdatedAt(OffsetDateTime.now());

        Driver saved = driverRepository.save(driver);

        // Audit Log
        auditLogService.log(actor, AuditAction.SOFT_DELETE, "Driver", saved.getId(), saved.getLicenseNumber(), null, oldMap, toMap(saved));
    }

    @Override
    @Transactional
    public DriverResponse reactivateDriver(Long id, Long userId) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));

        if (driver.getIsActive()) {
            return mapper.toResponse(driver);
        }

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Map<String, Object> oldMap = toMap(driver);

        driver.setIsActive(true);
        driver.setUpdatedBy(actor);
        driver.setUpdatedAt(OffsetDateTime.now());

        Driver saved = driverRepository.save(driver);

        // Audit Log
        auditLogService.log(actor, AuditAction.UPDATE, "Driver", saved.getId(), saved.getLicenseNumber(), null, oldMap, toMap(saved));

        return mapper.toResponse(saved);
    }

    private Map<String, Object> toMap(Driver d) {
        if (d == null) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("id", d.getId());
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
