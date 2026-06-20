package com.wms.dto.request;

import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripDepartRequest {

    private OffsetDateTime confirmedAt;

    @Size(max = 1000)
    private String notes;
}
