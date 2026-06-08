package com.wms.dto.response;

import com.wms.enums.UserRole;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

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
