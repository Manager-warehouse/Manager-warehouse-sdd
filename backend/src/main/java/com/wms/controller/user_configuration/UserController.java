package com.wms.controller.user_configuration;


import com.wms.dto.request.UserRequest;
import com.wms.dto.request.UserStatusRequest;
import com.wms.dto.response.UserResponse;
import com.wms.entity.access_control.User;
import com.wms.repository.UserRepository;
import com.wms.service.user_configuration.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller quản lý tài khoản người dùng (Spec 001) — chỉ dành cho ADMIN.
 * CRUD user, gán kho, bật/tắt trạng thái, xóa mềm.
 * Mỗi thao tác đều ghi audit log.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin User Management", description = "Quản lý tài khoản, vai trò và gán kho cho nhân viên (chỉ dành cho System Admin)")
@PreAuthorize("hasRole('ADMIN')")
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

    /** Lấy ID của admin đang thao tác từ JWT principal — dùng để ghi audit log. */
    private Long getAdminUserId(Principal principal) {
        String email = principal != null ? principal.getName() : "admin@phucanh.vn";
        User adminUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        return adminUser.getId();
    }
}
