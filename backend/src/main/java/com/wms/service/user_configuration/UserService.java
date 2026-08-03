package com.wms.service.user_configuration;


import com.wms.dto.request.UserRequest;
import com.wms.dto.response.UserResponse;

import java.util.List;

/**
 * Interface quản lý tài khoản người dùng (Spec 001).
 * Triển khai: UserServiceImpl
 * Gọi bởi: UserController
 */
public interface UserService {
    List<UserResponse> getAllUsers();
    UserResponse getUserById(Long id);
    UserResponse createUser(UserRequest request, Long adminUserId);
    UserResponse updateUser(Long id, UserRequest request, Long adminUserId);
    UserResponse toggleUserStatus(Long id, Boolean isActive, Long adminUserId);
    UserResponse softDeleteUser(Long id, Long adminUserId);
}
