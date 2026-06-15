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
}
