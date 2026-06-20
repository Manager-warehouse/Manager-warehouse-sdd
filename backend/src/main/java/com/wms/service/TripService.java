package com.wms.service;

import com.wms.dto.request.TripCancelRequest;
import com.wms.dto.request.TripCompleteRequest;
import com.wms.dto.request.TripCreateRequest;
import com.wms.dto.request.TripDepartRequest;
import com.wms.dto.request.TripUpdateRequest;
import com.wms.dto.response.TripResponse;
import com.wms.entity.User;

public interface TripService {
    TripResponse createTrip(TripCreateRequest request, User actor);

    TripResponse updateTrip(Long id, TripUpdateRequest request, User actor);

    TripResponse cancelTrip(Long id, TripCancelRequest request, User actor);

    TripResponse departTrip(Long id, TripDepartRequest request, User actor);

    TripResponse completeTrip(Long id, TripCompleteRequest request, User actor);
}
