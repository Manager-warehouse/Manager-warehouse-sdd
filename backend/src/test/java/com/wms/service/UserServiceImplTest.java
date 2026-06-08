package com.wms.service;

import com.wms.dto.request.UserRequest;
import com.wms.dto.response.UserResponse;
import com.wms.entity.AuditLog;
import com.wms.entity.User;
import com.wms.entity.UserWarehouseAssignment;
import com.wms.entity.Warehouse;
import com.wms.enums.AuditAction;
import com.wms.enums.UserRole;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.AuditLogRepository;
import com.wms.repository.UserRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User adminUser;
    private User targetUser;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .code("ADM01")
                .fullName("Admin User")
                .email("admin@phucanh.vn")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();

        targetUser = User.builder()
                .id(2L)
                .code("USR01")
                .fullName("John Doe")
                .email("john@phucanh.vn")
                .role(UserRole.STOREKEEPER)
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        warehouse = new Warehouse();
        warehouse.setId(10L);
        warehouse.setCode("HP-01");
        warehouse.setName("Hai Phong");
    }

    @Test
    @DisplayName("Lấy danh sách người dùng thành công")
    void getAllUsers_success() {
        when(userRepository.findAll()).thenReturn(List.of(targetUser));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(2L)).thenReturn(List.of(10L));

        List<UserResponse> responses = userService.getAllUsers();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getEmail()).isEqualTo("john@phucanh.vn");
        assertThat(responses.get(0).getWarehouses()).containsExactly(10L);
    }

    @Test
    @DisplayName("Lấy thông tin người dùng theo ID thành công")
    void getUserById_success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(2L)).thenReturn(List.of(10L));

        UserResponse response = userService.getUserById(2L);

        assertThat(response.getEmail()).isEqualTo("john@phucanh.vn");
        assertThat(response.getWarehouses()).containsExactly(10L);
    }

    @Test
    @DisplayName("Lấy thông tin người dùng theo ID thất bại - Không tìm thấy")
    void getUserById_notFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Tạo người dùng mới thành công và ghi nhận Audit Log")
    void createUser_success() {
        UserRequest request = UserRequest.builder()
                .code("NEW01")
                .fullName("New User")
                .email("new@phucanh.vn")
                .password("Password123")
                .role(UserRole.STOREKEEPER)
                .warehouses(List.of(10L))
                .build();

        when(userRepository.findByEmail("new@phucanh.vn")).thenReturn(Optional.empty());
        when(userRepository.findByCode("NEW01")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.encode("Password123")).thenReturn("hashedPassword");
        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));

        User savedUser = User.builder()
                .id(3L)
                .code("NEW01")
                .fullName("New User")
                .email("new@phucanh.vn")
                .role(UserRole.STOREKEEPER)
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(10L));

        UserResponse response = userService.createUser(request, 1L);

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getEmail()).isEqualTo("new@phucanh.vn");

        verify(userWarehouseAssignmentRepository).save(any(UserWarehouseAssignment.class));

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog auditLog = auditCaptor.getValue();
        assertThat(auditLog.getAction()).isEqualTo(AuditAction.CREATE);
        assertThat(auditLog.getEntityType()).isEqualTo("User");
        assertThat(auditLog.getActor().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Tạo người dùng mới thất bại - Email bị trùng")
    void createUser_emailTaken_throwsException() {
        UserRequest request = UserRequest.builder()
                .code("NEW01")
                .fullName("New User")
                .email("john@phucanh.vn")
                .password("Password123")
                .role(UserRole.STOREKEEPER)
                .build();

        when(userRepository.findByEmail("john@phucanh.vn")).thenReturn(Optional.of(targetUser));

        assertThatThrownBy(() -> userService.createUser(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EMAIL_TAKEN");
    }

    @Test
    @DisplayName("Tạo người dùng mới thất bại - Mã nhân viên bị trùng")
    void createUser_codeTaken_throwsException() {
        UserRequest request = UserRequest.builder()
                .code("USR01")
                .fullName("New User")
                .email("new@phucanh.vn")
                .password("Password123")
                .role(UserRole.STOREKEEPER)
                .build();

        when(userRepository.findByEmail("new@phucanh.vn")).thenReturn(Optional.empty());
        when(userRepository.findByCode("USR01")).thenReturn(Optional.of(targetUser));

        assertThatThrownBy(() -> userService.createUser(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CODE_TAKEN");
    }

    @Test
    @DisplayName("Tạo người dùng mới thất bại - Mật khẩu yếu")
    void createUser_weakPassword_throwsException() {
        UserRequest request = UserRequest.builder()
                .code("NEW01")
                .fullName("New User")
                .email("new@phucanh.vn")
                .password("weak")
                .role(UserRole.STOREKEEPER)
                .build();

        when(userRepository.findByEmail("new@phucanh.vn")).thenReturn(Optional.empty());
        when(userRepository.findByCode("NEW01")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.createUser(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("WEAK_PASSWORD");
    }

    @Test
    @DisplayName("Cập nhật thông tin người dùng thành công và ghi nhận Audit Log")
    void updateUser_success() {
        UserRequest request = UserRequest.builder()
                .code("USR01")
                .fullName("John Doe Updated")
                .email("john@phucanh.vn")
                .password("NewPassword123")
                .role(UserRole.WAREHOUSE_MANAGER)
                .warehouses(List.of(10L))
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(userRepository.findByEmail("john@phucanh.vn")).thenReturn(Optional.of(targetUser));
        when(userRepository.findByCode("USR01")).thenReturn(Optional.of(targetUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.encode("NewPassword123")).thenReturn("newHashedPassword");
        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));
        when(userRepository.save(any(User.class))).thenReturn(targetUser);
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(2L)).thenReturn(List.of(10L));

        UserResponse response = userService.updateUser(2L, request, 1L);

        assertThat(response.getFullName()).isEqualTo("John Doe Updated");
        assertThat(response.getRole()).isEqualTo(UserRole.WAREHOUSE_MANAGER);

        verify(userWarehouseAssignmentRepository).deleteByUserId(2L);
        verify(userWarehouseAssignmentRepository).save(any(UserWarehouseAssignment.class));

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo(AuditAction.UPDATE);
    }

    @Test
    @DisplayName("Thay đổi trạng thái hoạt động của người dùng thành công và ghi nhận Audit Log")
    void toggleUserStatus_success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any(User.class))).thenReturn(targetUser);
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(2L)).thenReturn(Collections.emptyList());

        UserResponse response = userService.toggleUserStatus(2L, false, 1L);

        assertThat(response.getIsActive()).isFalse();

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo(AuditAction.STATUS_CHANGE);
    }

    @Test
    @DisplayName("Xóa mềm người dùng thành công và ghi nhận Audit Log")
    void softDeleteUser_success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any(User.class))).thenReturn(targetUser);
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(2L)).thenReturn(Collections.emptyList());

        UserResponse response = userService.softDeleteUser(2L, 1L);

        assertThat(response.getIsActive()).isFalse();

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo(AuditAction.SOFT_DELETE);
    }
}
