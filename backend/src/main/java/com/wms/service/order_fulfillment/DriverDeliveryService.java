package com.wms.service.order_fulfillment;


import com.wms.entity.access_control.*;
import com.wms.entity.audit_trail.*;
import com.wms.entity.billing_payment.*;
import com.wms.entity.dealer_management.*;
import com.wms.entity.document_numbering.*;
import com.wms.entity.driver_management.*;
import com.wms.entity.fleet_management.*;
import com.wms.entity.notification_delivery.*;
import com.wms.entity.order_fulfillment.*;
import com.wms.entity.price_management.*;
import com.wms.entity.product_catalog.*;
import com.wms.entity.stock_control.*;
import com.wms.entity.stock_counting.*;
import com.wms.entity.stock_receiving.*;
import com.wms.entity.supplier_management.*;
import com.wms.entity.user_configuration.*;
import com.wms.entity.warehouse_location.*;
import com.wms.entity.warehouse_transfer.*;
import com.wms.enums.access_control.*;
import com.wms.enums.audit_trail.*;
import com.wms.enums.billing_payment.*;
import com.wms.enums.dealer_management.*;
import com.wms.enums.driver_management.*;
import com.wms.enums.fleet_management.*;
import com.wms.enums.notification_delivery.*;
import com.wms.enums.order_fulfillment.*;
import com.wms.enums.price_management.*;
import com.wms.enums.stock_control.*;
import com.wms.enums.stock_counting.*;
import com.wms.enums.stock_receiving.*;
import com.wms.enums.supplier_management.*;
import com.wms.enums.user_configuration.*;
import com.wms.enums.warehouse_location.*;
import com.wms.enums.warehouse_transfer.*;
import com.wms.dto.request.ConfirmDeliveryRequest;
import com.wms.dto.request.DeliveryOtpRequest;
import com.wms.dto.request.FailDeliveryRequest;
import com.wms.dto.request.ResetDeliveryOtpRequest;
import com.wms.dto.request.TripCompleteRequest;
import com.wms.dto.response.DeliveryAttemptResponse;
import com.wms.dto.response.DeliveryOtpResponse;
import com.wms.dto.response.TripDriverViewResponse;
import com.wms.entity.access_control.User;
import org.springframework.web.multipart.MultipartFile;

public interface DriverDeliveryService {
    TripDriverViewResponse getAssignedTrip(Long tripId, User actor);

    java.util.List<TripDriverViewResponse> listMyTrips(User actor);

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
