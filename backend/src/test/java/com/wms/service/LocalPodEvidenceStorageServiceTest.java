package com.wms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wms.exception.OutboundDeliveryException;
import com.wms.service.order_fulfillment.PodEvidenceStorageService.StoredPodContent;
import com.wms.service.order_fulfillment.PodEvidenceStorageService.StoredPodObject;
import com.wms.service.order_fulfillment.impl.LocalPodEvidenceStorageService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LocalPodEvidenceStorageServiceTest {

    @TempDir
    Path storageRoot;

    @Test
    void uploadAndRead_usesGeneratedRelativePathAndDetectedMetadata() throws Exception {
        LocalPodEvidenceStorageService storage = new LocalPodEvidenceStorageService(storageRoot.toString());
        byte[] jpeg = new byte[] { (byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00 };
        MockMultipartFile file = new MockMultipartFile(
                "goodsImage", "dealer-proof.exe", "application/octet-stream", jpeg);

        StoredPodObject stored = storage.upload(80L, "GOODS", file);
        StoredPodContent content = storage.read(stored.objectKey(), stored.originalFilename(), stored.contentType());

        assertThat(stored.objectKey()).matches("deliveries/80/goods-[0-9a-f-]+\\.jpg");
        assertThat(stored.originalFilename()).isEqualTo("dealer-proof.exe");
        assertThat(stored.contentType()).isEqualTo("image/jpeg");
        assertThat(stored.sizeBytes()).isEqualTo(jpeg.length);
        assertThat(content.bytes()).isEqualTo(jpeg);
        assertThat(content.originalFilename()).isEqualTo("dealer-proof.exe");
        assertThat(Files.exists(storageRoot.resolve(stored.objectKey()))).isTrue();
    }

    @Test
    void read_rejectsPathOutsideConfiguredRoot() {
        LocalPodEvidenceStorageService storage = new LocalPodEvidenceStorageService(storageRoot.toString());

        assertThatThrownBy(() -> storage.read("../secret.jpg", "secret.jpg", "image/jpeg"))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("POD_EVIDENCE_NOT_FOUND");
    }

    @Test
    void read_reportsMissingLocalFile() {
        LocalPodEvidenceStorageService storage = new LocalPodEvidenceStorageService(storageRoot.toString());

        assertThatThrownBy(() -> storage.read("deliveries/80/missing.jpg", "missing.jpg", "image/jpeg"))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("POD_EVIDENCE_NOT_FOUND");
    }

    @Test
    void delete_removesStoredFile() throws Exception {
        LocalPodEvidenceStorageService storage = new LocalPodEvidenceStorageService(storageRoot.toString());
        MockMultipartFile file = new MockMultipartFile(
                "signDocumentImage", "signed.png", "image/png",
                new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a });
        StoredPodObject stored = storage.upload(80L, "SIGNED_DOCUMENT", file);

        storage.delete(stored.objectKey());

        assertThat(Files.exists(storageRoot.resolve(stored.objectKey()))).isFalse();
    }

    @Test
    void upload_reportsStorageUnavailableWhenRootCannotBeCreated() throws Exception {
        Path rootFile = storageRoot.resolve("not-a-directory");
        Files.writeString(rootFile, "occupied");
        LocalPodEvidenceStorageService storage = new LocalPodEvidenceStorageService(rootFile.toString());
        MockMultipartFile file = new MockMultipartFile(
                "goodsImage", "goods.jpg", "image/jpeg",
                new byte[] { (byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00 });

        assertThatThrownBy(() -> storage.upload(80L, "GOODS", file))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("POD_STORAGE_UNAVAILABLE");
    }
}
