package com.wms.service.impl;

import com.wms.dto.request.UserRequest;
import com.wms.dto.response.UserResponse;
import com.wms.entity.User;
import com.wms.entity.UserWarehouseAssignment;
import com.wms.entity.Warehouse;
import com.wms.enums.AuditAction;
import com.wms.enums.UserRole;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.UserRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.service.AuditLogService;
import com.wms.service.UserService;
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
    private final AuditLogService auditLogService;
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

        if (request.getWarehouses() != null && !request.getWarehouses().isEmpty()) {
            saveWarehouseAssignments(savedUser, request.getWarehouses(), adminUser);
        }

        auditLogService.log(adminUser, AuditAction.CREATE, "User",
                savedUser.getId(), savedUser.getEmail(), null, null,
                Map.of("email", savedUser.getEmail(), "role", savedUser.getRole().name()));

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
        if (request.getWarehouses() != null && !request.getWarehouses().isEmpty()) {
            saveWarehouseAssignments(savedUser, request.getWarehouses(), adminUser);
        }

        auditLogService.log(adminUser, AuditAction.UPDATE, "User",
                savedUser.getId(), savedUser.getEmail(), null,
                Map.of("fullName", oldFullName, "role", oldRole.name()),
                Map.of("fullName", savedUser.getFullName(), "role", savedUser.getRole().name()));

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

        auditLogService.log(adminUser, AuditAction.STATUS_CHANGE, "User",
                savedUser.getId(), savedUser.getEmail(), null,
                Map.of("isActive", oldIsActive),
                Map.of("isActive", savedUser.getIsActive()));

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

        auditLogService.log(adminUser, AuditAction.SOFT_DELETE, "User",
                savedUser.getId(), savedUser.getEmail(), null,
                Map.of("isActive", true),
                Map.of("isActive", false));

        List<Long> assignedWarehouseIds = userWarehouseAssignmentRepository.findWarehouseIdsByUserId(savedUser.getId());
        return mapToResponse(savedUser, assignedWarehouseIds);
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
