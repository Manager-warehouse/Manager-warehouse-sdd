package com.wms.service;


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
import com.wms.service.user_configuration.*;
import com.wms.service.user_configuration.impl.*;
import com.wms.service.audit_trail.*;
import com.wms.service.access_control.*;
import com.wms.service.dealer_management.*;
import com.wms.service.dealer_management.impl.*;
import com.wms.service.billing_payment.*;
import com.wms.service.billing_payment.impl.*;
import com.wms.service.stock_receiving.*;
import com.wms.service.stock_control.*;
import com.wms.service.stock_control.impl.*;
import com.wms.service.notification_delivery.*;
import com.wms.service.notification_delivery.impl.*;
import com.wms.service.order_fulfillment.*;
import com.wms.service.order_fulfillment.impl.*;
import com.wms.service.price_management.*;
import com.wms.service.price_management.impl.*;
import com.wms.service.reporting_alerting.*;
import com.wms.service.reporting_alerting.impl.*;
import com.wms.service.return_disposal.*;
import com.wms.service.stock_counting.*;
import com.wms.service.fleet_management.*;
import com.wms.service.fleet_management.impl.*;
import com.wms.service.warehouse_location.*;
import com.wms.service.warehouse_location.impl.*;

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
import com.wms.service.user_configuration.impl.UserServiceImpl;
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

    @Test
    @DisplayName("Tạo người dùng vai trò bị giới hạn thất bại - Không gán kho nào")
    void createUser_restrictedRole_noWarehouse_throwsException() {
        UserRequest request = UserRequest.builder()
                .code("NEW02")
                .fullName("Store Keeper")
                .email("sk@phucanh.vn")
                .password("Password123")
                .role(UserRole.STOREKEEPER)
                .warehouses(Collections.emptyList())
                .build();

        when(userRepository.findByEmail("sk@phucanh.vn")).thenReturn(Optional.empty());
        when(userRepository.findByCode("NEW02")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.createUser(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("WAREHOUSE_REQUIRED");
    }

    @Test
    @DisplayName("Tạo người dùng vai trò bị giới hạn thất bại - Gán nhiều hơn 1 kho")
    void createUser_restrictedRole_multipleWarehouses_throwsException() {
        UserRequest request = UserRequest.builder()
                .code("NEW03")
                .fullName("Store Keeper Multi")
                .email("skmulti@phucanh.vn")
                .password("Password123")
                .role(UserRole.STOREKEEPER)
                .warehouses(List.of(10L, 20L))
                .build();

        when(userRepository.findByEmail("skmulti@phucanh.vn")).thenReturn(Optional.empty());
        when(userRepository.findByCode("NEW03")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.createUser(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MULTIPLE_WAREHOUSES_NOT_ALLOWED");
    }

    @Test
    @DisplayName("Tạo ADMIN thành công không lưu gán kho ngay cả khi truyền danh sách kho")
    void createAdmin_ignoreWarehouses_success() {
        UserRequest request = UserRequest.builder()
                .code("NEW_ADM")
                .fullName("New Admin")
                .email("newadmin@phucanh.vn")
                .password("Password123")
                .role(UserRole.ADMIN)
                .warehouses(List.of(10L, 20L))
                .build();

        when(userRepository.findByEmail("newadmin@phucanh.vn")).thenReturn(Optional.empty());
        when(userRepository.findByCode("NEW_ADM")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.encode("Password123")).thenReturn("hashedPassword");

        User savedUser = User.builder()
                .id(4L)
                .code("NEW_ADM")
                .fullName("New Admin")
                .email("newadmin@phucanh.vn")
                .role(UserRole.ADMIN)
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(4L)).thenReturn(Collections.emptyList());

        UserResponse response = userService.createUser(request, 1L);

        assertThat(response.getId()).isEqualTo(4L);
        assertThat(response.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(response.getWarehouses()).isEmpty();
        verify(userWarehouseAssignmentRepository, never()).save(any(UserWarehouseAssignment.class));
    }
}

