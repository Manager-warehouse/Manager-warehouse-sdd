package com.wms.dto.response;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PodEvidenceSignedUrlResponse {
    private String signedUrl;
    private String evidenceType;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private OffsetDateTime uploadedAt;
}
