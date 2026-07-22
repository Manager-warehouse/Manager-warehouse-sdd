package com.wms.service.user_configuration.impl;


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
import com.wms.dto.request.UserRequest;
import com.wms.dto.response.UserResponse;
import com.wms.entity.audit_trail.AuditLog;
import com.wms.entity.access_control.User;
import com.wms.entity.access_control.UserWarehouseAssignment;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.AuditLogRepository;
import com.wms.repository.UserRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.service.user_configuration.UserService;
import com.wms.util.AuditLogUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;
    private final WarehouseRepository warehouseRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> {
                    List<Long> warehouseIds = userWarehouseAssignmentRepository.findWarehouseIdsByUserId(user.getId());
                    return mapToResponse(user, warehouseIds);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        List<Long> warehouseIds = userWarehouseAssignmentRepository.findWarehouseIdsByUserId(user.getId());
        return mapToResponse(user, warehouseIds);
    }

    @Override
    @Transactional
    public UserResponse createUser(UserRequest request, Long adminUserId) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("EMAIL_TAKEN");
        }
        if (userRepository.findByCode(request.getCode()).isPresent()) {
            throw new IllegalArgumentException("CODE_TAKEN");
        }

        validatePasswordStrength(request.getPassword());
        validateWarehouseAssignments(request.getRole(), request.getWarehouses());

        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found with id: " + adminUserId));

        User user = User.builder()
                .code(request.getCode())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .jobTitle(request.getJobTitle())
                .shift(request.getShift())
                .region(request.getRegion())
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        if (request.getRole() != UserRole.ADMIN && request.getRole() != UserRole.CEO) {
            if (request.getWarehouses() != null && !request.getWarehouses().isEmpty()) {
                saveWarehouseAssignments(savedUser, request.getWarehouses(), adminUser);
            }
        }

        // Record Audit Log
        AuditLog auditLog = AuditLog.builder()
                .actor(adminUser)
                .actorRole(adminUser.getRole() != null ? adminUser.getRole().name() : "ADMIN")
                .action(AuditAction.CREATE)
                .entityType("User")
                .entityId(savedUser.getId())
                .description("CREATE User " + savedUser.getId())
                .newValue(AuditLogUtil.toJson(Map.of("email", savedUser.getEmail(), "role", savedUser.getRole().name())))
                .timestamp(OffsetDateTime.now())
                .build();
        auditLogRepository.save(auditLog);

        List<Long> assignedWarehouseIds = userWarehouseAssignmentRepository.findWarehouseIdsByUserId(savedUser.getId());
        return mapToResponse(savedUser, assignedWarehouseIds);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserRequest request, Long adminUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (userRepository.findByEmail(request.getEmail()).filter(u -> !u.getId().equals(id)).isPresent()) {
            throw new IllegalArgumentException("EMAIL_TAKEN");
        }
        if (userRepository.findByCode(request.getCode()).filter(u -> !u.getId().equals(id)).isPresent()) {
            throw new IllegalArgumentException("CODE_TAKEN");
        }

        validateWarehouseAssignments(request.getRole(), request.getWarehouses());

        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found with id: " + adminUserId));

        String oldFullName = user.getFullName();
        UserRole oldRole = user.getRole();

        user.setCode(request.getCode());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
        user.setJobTitle(request.getJobTitle());
        user.setShift(request.getShift());
        user.setRegion(request.getRegion());
        user.setUpdatedAt(OffsetDateTime.now());

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            validatePasswordStrength(request.getPassword());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        User savedUser = userRepository.save(user);

        // Delete old assignments and save new ones
        userWarehouseAssignmentRepository.deleteByUserId(id);
        if (request.getRole() != UserRole.ADMIN && request.getRole() != UserRole.CEO) {
            if (request.getWarehouses() != null && !request.getWarehouses().isEmpty()) {
                saveWarehouseAssignments(savedUser, request.getWarehouses(), adminUser);
            }
        }

        // Record Audit Log
        AuditLog auditLog = AuditLog.builder()
                .actor(adminUser)
                .actorRole(adminUser.getRole() != null ? adminUser.getRole().name() : "ADMIN")
                .action(AuditAction.UPDATE)
                .entityType("User")
                .entityId(savedUser.getId())
                .description("UPDATE User " + savedUser.getId())
                .oldValue(AuditLogUtil.toJson(Map.of("fullName", oldFullName, "role", oldRole.name())))
                .newValue(AuditLogUtil.toJson(Map.of("fullName", savedUser.getFullName(), "role", savedUser.getRole().name())))
                .timestamp(OffsetDateTime.now())
                .build();
        auditLogRepository.save(auditLog);

        List<Long> assignedWarehouseIds = userWarehouseAssignmentRepository.findWarehouseIdsByUserId(savedUser.getId());
        return mapToResponse(savedUser, assignedWarehouseIds);
    }

    @Override
    @Transactional
    public UserResponse toggleUserStatus(Long id, Boolean isActive, Long adminUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (user.getIsActive().equals(isActive)) {
            List<Long> assignedWarehouseIds = userWarehouseAssignmentRepository.findWarehouseIdsByUserId(user.getId());
            return mapToResponse(user, assignedWarehouseIds);
        }

        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found with id: " + adminUserId));

        Boolean oldIsActive = user.getIsActive();
        user.setIsActive(isActive);
        user.setUpdatedAt(OffsetDateTime.now());
        User savedUser = userRepository.save(user);

        // Record Audit Log
        AuditLog auditLog = AuditLog.builder()
                .actor(adminUser)
                .actorRole(adminUser.getRole() != null ? adminUser.getRole().name() : "ADMIN")
                .action(AuditAction.STATUS_CHANGE)
                .entityType("User")
                .entityId(savedUser.getId())
                .description("STATUS_CHANGE User " + savedUser.getId())
                .oldValue(AuditLogUtil.toJson(Map.of("isActive", oldIsActive)))
                .newValue(AuditLogUtil.toJson(Map.of("isActive", savedUser.getIsActive())))
                .timestamp(OffsetDateTime.now())
                .build();
        auditLogRepository.save(auditLog);

        List<Long> assignedWarehouseIds = userWarehouseAssignmentRepository.findWarehouseIdsByUserId(savedUser.getId());
        return mapToResponse(savedUser, assignedWarehouseIds);
    }

    @Override
    @Transactional
    public UserResponse softDeleteUser(Long id, Long adminUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (!user.getIsActive()) {
            List<Long> assignedWarehouseIds = userWarehouseAssignmentRepository.findWarehouseIdsByUserId(user.getId());
            return mapToResponse(user, assignedWarehouseIds);
        }

        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found with id: " + adminUserId));

        user.setIsActive(false);
        user.setUpdatedAt(OffsetDateTime.now());
        User savedUser = userRepository.save(user);

        // Record Audit Log
        AuditLog auditLog = AuditLog.builder()
                .actor(adminUser)
                .actorRole(adminUser.getRole() != null ? adminUser.getRole().name() : "ADMIN")
                .action(AuditAction.SOFT_DELETE)
                .entityType("User")
                .entityId(savedUser.getId())
                .description("SOFT_DELETE User " + savedUser.getId())
                .oldValue(AuditLogUtil.toJson(Map.of("isActive", true)))
                .newValue(AuditLogUtil.toJson(Map.of("isActive", false)))
                .timestamp(OffsetDateTime.now())
                .build();
        auditLogRepository.save(auditLog);

        List<Long> assignedWarehouseIds = userWarehouseAssignmentRepository.findWarehouseIdsByUserId(savedUser.getId());
        return mapToResponse(savedUser, assignedWarehouseIds);
    }

    private void validateWarehouseAssignments(UserRole role, List<Long> warehouses) {
        if (role != UserRole.ADMIN && role != UserRole.CEO) {
            if (warehouses == null || warehouses.isEmpty()) {
                throw new IllegalArgumentException("WAREHOUSE_REQUIRED");
            }
            if (warehouses.size() > 1) {
                throw new IllegalArgumentException("MULTIPLE_WAREHOUSES_NOT_ALLOWED");
            }
        }
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("WEAK_PASSWORD");
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        if (!hasLetter || !hasDigit) {
            throw new IllegalArgumentException("WEAK_PASSWORD");
        }
    }

    private void saveWarehouseAssignments(User user, List<Long> warehouseIds, User adminUser) {
        for (Long warehouseId : warehouseIds) {
            Warehouse warehouse = warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + warehouseId));
            UserWarehouseAssignment assignment = new UserWarehouseAssignment();
            assignment.setUser(user);
            assignment.setWarehouse(warehouse);
            assignment.setAssignedBy(adminUser);
            assignment.setAssignedAt(OffsetDateTime.now());
            userWarehouseAssignmentRepository.save(assignment);
        }
    }

    private UserResponse mapToResponse(User user, List<Long> warehouseIds) {
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
                .warehouses(warehouseIds)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
