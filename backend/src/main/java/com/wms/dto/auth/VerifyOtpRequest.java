package com.wms.dto.auth;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** DTO xác thực OTP và đặt mật khẩu mới (Spec 001). */
@Getter
@Setter
public class VerifyOtpRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "OTP phải là 6 chữ số")
    private String otp;

    @NotBlank
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "Mật khẩu phải chứa ít nhất 1 chữ hoa, 1 chữ thường và 1 chữ số")
    private String newPassword;
}
