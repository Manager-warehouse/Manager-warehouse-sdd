package com.wms.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    // @Bean
    // @ConditionalOnMissingBean(JavaMailSender.class)
    // public JavaMailSender javaMailSender() {
    //     JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    //     mailSender.setHost("localhost");
    //     mailSender.setPort(25);

    //     Properties props = mailSender.getJavaMailProperties();
    //     props.put("mail.transport.protocol", "smtp");
    //     props.put("mail.smtp.auth", "false");
    //     props.put("mail.smtp.starttls.enable", "false");
    //     props.put("mail.debug", "false");

    //     return mailSender;
    //}
}
