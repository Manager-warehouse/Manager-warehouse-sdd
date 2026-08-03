package com.wms.service.billing_payment;


import com.wms.dto.response.PaymentReceiptOcrResponse;
import com.wms.entity.access_control.User;
import org.springframework.web.multipart.MultipartFile;

public interface OcrService {
    PaymentReceiptOcrResponse processOcr(MultipartFile file, User actor);

    // Runs the image through the OCR engine only and returns the raw recognized text, so
    // other domains (e.g. supplier-payment OCR) can reuse the same engine/preprocessing
    // without inheriting the dealer-matching logic tied to processOcr's response shape.
    String extractRawText(MultipartFile file);
}
