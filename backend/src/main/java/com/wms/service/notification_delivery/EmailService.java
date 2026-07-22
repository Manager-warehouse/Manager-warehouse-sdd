package com.wms.service.notification_delivery;


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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Async
    public void sendOtpEmail(String to, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Mã OTP đặt lại mật khẩu — WMS Phúc Anh");
            message.setText("Mã OTP của bạn là: " + otp + "\nMã có hiệu lực trong 10 phút. Không chia sẻ mã này với bất kỳ ai.");
            mailSender.send(message);
            log.info("Đã gửi email OTP tới {}", to);
        } catch (Exception e) {
            log.error("Không thể gửi email OTP (SMTP error): {}", e.getMessage(), e);
        }
    }

    // ACC-02 (business.md): hóa đơn quá hạn tự động CREDIT_HOLD phải cảnh báo Kế toán trưởng.
    @Async
    public void sendCreditHoldAlert(String to, String dealerCode, String dealerName,
            String invoiceNumber, long overdueDays) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("[CẢNH BÁO] Đại lý " + dealerCode + " bị khóa tín dụng (CREDIT_HOLD) — WMS Phúc Anh");
            message.setText("Đại lý " + dealerName + " (" + dealerCode + ") đã tự động chuyển sang trạng thái "
                    + "CREDIT_HOLD do hóa đơn " + invoiceNumber + " quá hạn thanh toán " + overdueDays + " ngày.\n"
                    + "Vui lòng kiểm tra công nợ đại lý trên hệ thống WMS.");
            mailSender.send(message);
            log.info("Đã gửi email cảnh báo CREDIT_HOLD tới {}", to);
        } catch (Exception e) {
            log.error("Không thể gửi email cảnh báo CREDIT_HOLD (SMTP error): {}", e.getMessage(), e);
        }
    }
}
