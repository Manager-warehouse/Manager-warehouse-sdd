package com.wms.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/** DTO kiểm tra OTP — chứa email và mã OTP 6 số (Spec 001). */
@Getter
@Setter
public class CheckOtpRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "OTP phải là 6 chữ số")
    private String otp;
}
