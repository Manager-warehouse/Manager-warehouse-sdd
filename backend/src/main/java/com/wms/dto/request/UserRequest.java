package com.wms.dto.request;


import com.wms.enums.access_control.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO tạo/cập nhật tài khoản — chứa thông tin nhân viên, role, kho được gán (Spec 001). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequest {

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String phone;

    private String password; // optional on update, required on create

    @NotNull(message = "Role is required")
    private UserRole role;

    private String jobTitle;
    private String shift;
    private String region;

    private List<Long> warehouses;
}
