package com.wms.service.order_fulfillment.impl;

import com.wms.exception.OutboundDeliveryException;
import com.wms.service.order_fulfillment.PodEvidenceStorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalPodEvidenceStorageService implements PodEvidenceStorageService {

    private final Path storageRoot;

    public LocalPodEvidenceStorageService(
            @Value("${wms.pod-storage.root:${POD_STORAGE_ROOT:../data/pod-evidence}}") String storageRoot) {
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    @Override
    public StoredPodObject upload(Long deliveryId, String evidenceType, MultipartFile file) {
        String normalizedType = normalizeEvidenceType(evidenceType);
        String contentType = detectContentType(file);
        String relativePath = "deliveries/" + deliveryId + "/"
                + normalizedType.toLowerCase(Locale.ROOT) + "-" + UUID.randomUUID() + extension(contentType);
        Path target = resolveInsideRoot(relativePath);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, file.getBytes(), StandardOpenOption.CREATE_NEW);
            return new StoredPodObject(relativePath.replace('\\', '/'), sanitizeOriginalFilename(file),
                    contentType, file.getSize());
        } catch (IOException | RuntimeException ex) {
            throw storageUnavailable("Unable to store POD evidence", ex);
        }
    }

    @Override
    public void delete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolveInsideRoot(objectKey));
        } catch (IOException ex) {
            throw storageUnavailable("Unable to delete POD evidence", ex);
        }
    }

    @Override
    public StoredPodContent read(String objectKey, String originalFilename, String contentType) {
        Path source = resolveInsideRoot(objectKey);
        try {
            if (!Files.isRegularFile(source) || !Files.isReadable(source)) {
                throw evidenceNotFound();
            }
            return new StoredPodContent(Files.readAllBytes(source), sanitizeFilename(originalFilename), contentType);
        } catch (OutboundDeliveryException ex) {
            throw ex;
        } catch (IOException ex) {
            throw evidenceNotFound();
        }
    }

    private Path resolveInsideRoot(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw evidenceNotFound();
        }
        Path resolved = storageRoot.resolve(objectKey).normalize();
        if (!resolved.startsWith(storageRoot)) {
            throw evidenceNotFound();
        }
        return resolved;
    }

    private String detectContentType(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            if (isPng(bytes)) return "image/png";
            if (isJpeg(bytes)) return "image/jpeg";
            if (isWebp(bytes)) return "image/webp";
        } catch (IOException ex) {
            throw invalidFile(ex);
        }
        throw invalidFile(null);
    }

    private String normalizeEvidenceType(String evidenceType) {
        String value = evidenceType == null ? "" : evidenceType.toUpperCase(Locale.ROOT);
        if (!value.equals("GOODS") && !value.equals("SIGNED_DOCUMENT")) {
            throw invalidFile(null);
        }
        return value;
    }

    private String sanitizeOriginalFilename(MultipartFile file) {
        return sanitizeFilename(file.getOriginalFilename());
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "pod-evidence";
        }
        String normalized = filename.replace('\\', '/');
        String leaf = normalized.substring(normalized.lastIndexOf('/') + 1);
        return leaf.replaceAll("[\\r\\n\\\"]", "_");
    }

    private boolean isPng(byte[] bytes) {
        return bytes.length >= 8 && (bytes[0] & 0xff) == 0x89
                && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47;
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3 && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
    }

    private boolean isWebp(byte[] bytes) {
        return bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I'
                && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W'
                && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }

    private String extension(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    private OutboundDeliveryException invalidFile(Exception cause) {
        OutboundDeliveryException exception = new OutboundDeliveryException(
                "POD_FILE_INVALID", HttpStatus.BAD_REQUEST,
                "POD evidence must be a valid JPEG, PNG, or WebP image");
        if (cause != null) exception.initCause(cause);
        return exception;
    }

    private OutboundDeliveryException evidenceNotFound() {
        return new OutboundDeliveryException(
                "POD_EVIDENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "POD evidence was not found");
    }

    private OutboundDeliveryException storageUnavailable(String message, Exception cause) {
        OutboundDeliveryException exception = new OutboundDeliveryException(
                "POD_STORAGE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, message);
        exception.initCause(cause);
        return exception;
    }
}
