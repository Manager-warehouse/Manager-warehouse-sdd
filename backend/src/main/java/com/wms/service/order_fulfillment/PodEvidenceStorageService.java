package com.wms.service.order_fulfillment;

import org.springframework.web.multipart.MultipartFile;

public interface PodEvidenceStorageService {

    StoredPodObject upload(Long deliveryId, String evidenceType, MultipartFile file);

    void delete(String objectKey);

    StoredPodContent read(String objectKey, String originalFilename, String contentType);

    record StoredPodObject(String objectKey, String originalFilename, String contentType, long sizeBytes) {
    }

    record StoredPodContent(byte[] bytes, String originalFilename, String contentType) {
    }
}
