package com.wms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatusRequest {

    @NotNull(message = "isActive is required")
    private Boolean isActive;
}
