package com.wms.repository;

import com.wms.entity.DeliveryOtpAttempt;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryOtpAttemptRepository extends JpaRepository<DeliveryOtpAttempt, Long> {

    Optional<DeliveryOtpAttempt> findFirstByDeliveryIdAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            Long deliveryId,
            OffsetDateTime now);
}
