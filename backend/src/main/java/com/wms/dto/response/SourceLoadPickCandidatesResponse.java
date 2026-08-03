package com.wms.dto.response;

import java.util.List;

public record SourceLoadPickCandidatesResponse(
        Long transferId,
        List<SourceLoadPickItemResponse> items
) {}
