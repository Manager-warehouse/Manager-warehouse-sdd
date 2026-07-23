package com.wms.dto.request;


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
import static org.assertj.core.api.Assertions.assertThat;

import com.wms.dto.request.dealer_management.DealerCreateRequest;
import com.wms.dto.request.dealer_management.DealerUpdateRequest;
import com.wms.dto.request.product_catalog.ProductRequest;
import com.wms.dto.request.supplier_management.SupplierCreateRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void interWarehouseReason_requiresNonBlankReason() {
        InterWarehouseTransferReasonRequest request = new InterWarehouseTransferReasonRequest(" ");

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void finalReceive_allowsEmptyReasonButRejectsOversizedReason() {
        assertThat(validator.validate(new InterWarehouseTransferFinalReceiveRequest(null))).isEmpty();

        InterWarehouseTransferFinalReceiveRequest request =
                new InterWarehouseTransferFinalReceiveRequest("x".repeat(1001));

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void productRequest_rejectsInvalidNumericFields() {
        ProductRequest request = validProductRequest();
        request.setUnitPerPack(0);
        request.setWeightKg(BigDecimal.valueOf(-1));
        request.setVolumeM3(BigDecimal.valueOf(-1));
        request.setReorderPoint(BigDecimal.valueOf(-1));

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("unitPerPack", "weightKg", "volumeM3", "reorderPoint");
    }

    @Test
    void masterDataRequests_rejectOversizedAddresses() {
        DealerCreateRequest dealer = new DealerCreateRequest();
        dealer.setCode("DL-HN-01");
        dealer.setName("Dealer Ha Noi");
        dealer.setDefaultDeliveryAddress("x".repeat(1001));

        SupplierCreateRequest supplier = new SupplierCreateRequest();
        supplier.setCode("SUP-01");
        supplier.setCompanyName("Supplier");
        supplier.setAddress("x".repeat(1001));

        assertThat(validator.validate(dealer))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("defaultDeliveryAddress");
        assertThat(validator.validate(supplier))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("address");
    }

    @Test
    void dealerRequests_rejectInvalidEmail() {
        DealerCreateRequest createRequest = new DealerCreateRequest();
        createRequest.setCode("DL-HN-01");
        createRequest.setName("Dealer Ha Noi");
        createRequest.setEmail("not-an-email");

        DealerUpdateRequest updateRequest = new DealerUpdateRequest();
        updateRequest.setEmail("not-an-email");

        assertThat(validator.validate(createRequest))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("email");
        assertThat(validator.validate(updateRequest))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("email");
    }

    private ProductRequest validProductRequest() {
        ProductRequest request = new ProductRequest();
        request.setSku("SKU-001");
        request.setName("Noi inox");
        request.setUnit("cai");
        return request;
    }
}
