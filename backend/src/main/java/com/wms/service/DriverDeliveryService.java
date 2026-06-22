package com.wms.service;

import com.wms.dto.request.ConfirmDeliveryRequest;
import com.wms.dto.request.DeliveryOtpRequest;
import com.wms.dto.request.FailDeliveryRequest;
import com.wms.dto.request.ResetDeliveryOtpRequest;
import com.wms.dto.request.TripCompleteRequest;
import com.wms.dto.response.DeliveryAttemptResponse;
import com.wms.dto.response.DeliveryOtpResponse;
import com.wms.dto.response.TripDriverViewResponse;
import com.wms.entity.User;
import com.wms.enums.TripStatus;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface DriverDeliveryService {
    List<TripDriverViewResponse> listMyTrips(TripStatus status, User actor);

    TripDriverViewResponse getAssignedTrip(Long tripId, User actor);

    DeliveryAttemptResponse uploadPodEvidence(Long tripId, Long deliveryOrderId,
                                              MultipartFile goodsImage,
                                              MultipartFile signDocumentImage,
                                              String notes,
                                              User actor);

    DeliveryOtpResponse requestDeliveryOtp(Long tripId, Long deliveryOrderId,
                                           DeliveryOtpRequest request,
                                           User actor);

    DeliveryAttemptResponse confirmDelivery(Long tripId, Long deliveryOrderId,
                                            ConfirmDeliveryRequest request,
                                            User actor);

    DeliveryAttemptResponse failDelivery(Long tripId, Long deliveryOrderId,
                                         FailDeliveryRequest request,
                                         User actor);

    TripDriverViewResponse completeTrip(Long tripId, TripCompleteRequest request, User actor);

    DeliveryOtpResponse resetDeliveryOtp(Long deliveryOrderId, ResetDeliveryOtpRequest request, User actor);
}
