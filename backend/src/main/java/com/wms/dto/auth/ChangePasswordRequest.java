package com.wms.dto.auth;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** DTO yêu cầu đổi mật khẩu — chứa mật khẩu hiện tại và mật khẩu mới (Spec 001). */
@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank
    private String currentPassword;

    @NotBlank
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "Mật khẩu phải chứa ít nhất 1 chữ hoa, 1 chữ thường và 1 chữ số")
    private String newPassword;
}
