package com.nn.ticketapp_api.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender{

    private final JavaMailSender javaMailSender;
    private static final String SYSTEM_SENDER = "no-reply@ticketapp.local";

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(SYSTEM_SENDER);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            javaMailSender.send(message);
            log.debug("Successfully pushed HTML email to SMTP server for recipient: {}", to);

        } catch (MailException | MessagingException e) {
            log.error("Failed to send HTML email to: {} due to network or configuration error", to, e);
        }
    }
}
