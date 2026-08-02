package com.wms.dto.response;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PodEvidenceSignedUrlsResponse {
    private Long doId;
    private Long deliveryId;
    private OffsetDateTime expiresAt;
    private PodEvidenceSignedUrlResponse goodsImage;
    private PodEvidenceSignedUrlResponse signDocumentImage;
}
