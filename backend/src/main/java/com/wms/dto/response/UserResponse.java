package com.wms.dto.response;


import com.wms.enums.access_control.UserRole;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO phản hồi thông tin tài khoản — danh sách/chi tiết (Spec 001). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String code;
    private String fullName;
    private String email;
    private String phone;
    private UserRole role;
    private String jobTitle;
    private String shift;
    private String region;
    private Boolean isActive;
    private List<Long> warehouses;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
