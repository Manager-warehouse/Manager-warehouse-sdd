package com.wms.entity.order_fulfillment;

import com.wms.entity.driver_management.Driver;
import com.wms.entity.fleet_management.Vehicle;
import com.wms.enums.order_fulfillment.DeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_number", nullable = false, unique = true, length = 50)
    private String deliveryNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "do_id", nullable = false)
    private DeliveryOrder deliveryOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DeliveryStatus status;

    @Column(name = "pod_image_url", length = 500)
    private String podImageUrl;

    @Column(name = "pod_signature_url", length = 500)
    private String podSignatureUrl;

    @Column(name = "goods_image_object_key", length = 500)
    private String goodsImageObjectKey;

    @Column(name = "goods_image_original_filename", length = 255)
    private String goodsImageOriginalFilename;

    @Column(name = "goods_image_content_type", length = 100)
    private String goodsImageContentType;

    @Column(name = "goods_image_size_bytes")
    private Long goodsImageSizeBytes;

    @Column(name = "goods_image_uploaded_at")
    private OffsetDateTime goodsImageUploadedAt;

    @Column(name = "signed_document_object_key", length = 500)
    private String signedDocumentObjectKey;

    @Column(name = "signed_document_original_filename", length = 255)
    private String signedDocumentOriginalFilename;

    @Column(name = "signed_document_content_type", length = 100)
    private String signedDocumentContentType;

    @Column(name = "signed_document_size_bytes")
    private Long signedDocumentSizeBytes;

    @Column(name = "signed_document_uploaded_at")
    private OffsetDateTime signedDocumentUploadedAt;

    @Column(name = "pod_timestamp")
    private OffsetDateTime podTimestamp;

    @Column(name = "otp_verified_at")
    private OffsetDateTime otpVerifiedAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "dispatched_at")
    private OffsetDateTime dispatchedAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
