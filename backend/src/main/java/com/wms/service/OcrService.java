package com.wms.service;

import com.wms.dto.response.PaymentReceiptOcrResponse;
import com.wms.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface OcrService {
    PaymentReceiptOcrResponse processOcr(MultipartFile file, User actor);
}
