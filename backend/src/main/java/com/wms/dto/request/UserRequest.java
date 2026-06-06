package com.wms.dto.request;

import com.wms.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

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
