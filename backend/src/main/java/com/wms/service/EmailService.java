package com.wms.service;

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
