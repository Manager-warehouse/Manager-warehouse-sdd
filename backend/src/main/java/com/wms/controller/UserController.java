package com.wms.controller;

import com.wms.dto.request.UserRequest;
import com.wms.dto.request.UserStatusRequest;
import com.wms.dto.response.UserResponse;
import com.wms.entity.User;
import com.wms.repository.UserRepository;
import com.wms.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin User Management", description = "Quản lý tài khoản, vai trò và gán kho cho nhân viên (chỉ dành cho System Admin)")
@PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER')")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @Operation(summary = "Lấy danh sách người dùng")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(summary = "Lấy thông tin chi tiết người dùng")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @Operation(summary = "Tạo người dùng mới")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserRequest request,
            Principal principal) {
        Long adminUserId = getAdminUserId(principal);
        UserResponse response = userService.createUser(request, adminUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Cập nhật thông tin người dùng")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest request,
            Principal principal) {
        Long adminUserId = getAdminUserId(principal);
        UserResponse response = userService.updateUser(id, request, adminUserId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Xóa mềm người dùng (đặt isActive = false)")
    @DeleteMapping("/{id}")
    public ResponseEntity<UserResponse> softDeleteUser(
            @PathVariable Long id,
            Principal principal) {
        Long adminUserId = getAdminUserId(principal);
        UserResponse response = userService.softDeleteUser(id, adminUserId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Thay đổi trạng thái hoạt động của người dùng")
    @PutMapping("/{id}/status")
    public ResponseEntity<UserResponse> toggleUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest request,
            Principal principal) {
        Long adminUserId = getAdminUserId(principal);
        UserResponse response = userService.toggleUserStatus(id, request.getIsActive(), adminUserId);
        return ResponseEntity.ok(response);
    }

    private Long getAdminUserId(Principal principal) {
        String email = principal != null ? principal.getName() : "admin@phucanh.vn";
        User adminUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        return adminUser.getId();
    }
}
