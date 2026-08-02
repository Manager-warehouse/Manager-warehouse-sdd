package com.wms.service.order_fulfillment.impl;

import com.wms.exception.OutboundDeliveryException;
import com.wms.service.order_fulfillment.PodEvidenceStorageService;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SupabasePodEvidenceStorageService implements PodEvidenceStorageService {

    private final RestClient restClient;
    private final String baseUrl;
    private final String serviceKey;
    private final String bucket;

    public SupabasePodEvidenceStorageService(
            RestClient.Builder restClientBuilder,
            @Value("${wms.pod-storage.supabase-url:}") String baseUrl,
            @Value("${wms.pod-storage.service-key:}") String serviceKey,
            @Value("${wms.pod-storage.bucket:pod-evidence}") String bucket) {
        this.restClient = restClientBuilder.build();
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.serviceKey = serviceKey;
        this.bucket = bucket;
    }

    @Override
    public StoredPodObject upload(Long deliveryId, String evidenceType, MultipartFile file) {
        requireConfigured();
        String contentType = detectContentType(file);
        String objectKey = "deliveries/" + deliveryId + "/" + evidenceType.toLowerCase()
                + "-" + UUID.randomUUID() + extension(contentType);
        try {
            restClient.post()
                    .uri(objectUrl(objectKey))
                    .header("Authorization", "Bearer " + serviceKey)
                    .header("apikey", serviceKey)
                    .header("Content-Type", contentType)
                    .header("x-upsert", "false")
                    .body(file.getBytes())
                    .retrieve()
                    .toBodilessEntity();
            return new StoredPodObject(objectKey, file.getOriginalFilename(), contentType, file.getSize());
        } catch (IOException | RuntimeException ex) {
            throw storageFailure("POD_STORAGE_UPLOAD_FAILED", "Unable to upload POD evidence", ex);
        }
    }

    @Override
    public void delete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        requireConfigured();
        try {
            restClient.delete()
                    .uri(objectUrl(objectKey))
                    .header("Authorization", "Bearer " + serviceKey)
                    .header("apikey", serviceKey)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException ex) {
            throw storageFailure("POD_STORAGE_DELETE_FAILED", "Unable to delete POD evidence", ex);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public String createSignedUrl(String objectKey, int expiresInSeconds) {
        requireConfigured();
        try {
            Map<String, Object> response = restClient.post()
                    .uri(baseUrl + "/storage/v1/object/sign/" + bucket + "/" + objectKey)
                    .header("Authorization", "Bearer " + serviceKey)
                    .header("apikey", serviceKey)
                    .body(Map.of("expiresIn", expiresInSeconds))
                    .retrieve()
                    .body(Map.class);
            Object value = response == null ? null : response.get("signedURL");
            if (value == null) {
                throw new IllegalStateException("Supabase did not return signedURL");
            }
            String signedUrl = value.toString();
            return signedUrl.startsWith("http") ? signedUrl : baseUrl + "/storage/v1" + signedUrl;
        } catch (RuntimeException ex) {
            throw storageFailure("POD_SIGNED_URL_FAILED", "Unable to create POD signed URL", ex);
        }
    }

    private String objectUrl(String objectKey) {
        return baseUrl + "/storage/v1/object/" + bucket + "/" + objectKey;
    }

    private void requireConfigured() {
        if (baseUrl.isBlank() || serviceKey.isBlank()) {
            throw new OutboundDeliveryException("POD_STORAGE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                    "Private POD storage is not configured");
        }
    }

    private String detectContentType(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            if (isPng(bytes)) return "image/png";
            if (isJpeg(bytes)) return "image/jpeg";
            if (isWebp(bytes)) return "image/webp";
        } catch (IOException ex) {
            throw storageFailure("POD_FILE_INVALID", "Unable to read POD image", ex);
        }
        throw new OutboundDeliveryException("POD_FILE_INVALID", HttpStatus.BAD_REQUEST,
                "POD evidence must be a valid JPEG, PNG, or WebP image");
    }

    private boolean isPng(byte[] b) {
        return b.length >= 8 && (b[0] & 0xff) == 0x89 && b[1] == 0x50 && b[2] == 0x4e && b[3] == 0x47;
    }

    private boolean isJpeg(byte[] b) {
        return b.length >= 3 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xd8 && (b[2] & 0xff) == 0xff;
    }

    private boolean isWebp(byte[] b) {
        return b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
    }

    private String extension(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    private OutboundDeliveryException storageFailure(String code, String message, Exception cause) {
        OutboundDeliveryException exception = new OutboundDeliveryException(code, HttpStatus.BAD_GATEWAY, message);
        exception.initCause(cause);
        return exception;
    }

    private static String stripTrailingSlash(String value) {
        return value == null ? "" : value.replaceFirst("/+$", "");
    }
}
