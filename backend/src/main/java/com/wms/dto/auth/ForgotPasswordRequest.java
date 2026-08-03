package com.wms.dto.auth;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** DTO yêu cầu quên mật khẩu — chứa email để gửi OTP (Spec 001). */
@Getter
@Setter
public class ForgotPasswordRequest {

    @NotBlank
    @Email
    private String email;
}
