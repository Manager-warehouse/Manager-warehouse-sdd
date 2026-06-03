package com.wms.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank
    private String currentPassword;

    @NotBlank
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    private String newPassword;
}
