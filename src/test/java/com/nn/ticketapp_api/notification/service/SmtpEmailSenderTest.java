package com.nn.ticketapp_api.notification.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class SmtpEmailSenderTest {

    @Mock
    private JavaMailSender javaMailSender;
    @InjectMocks
    private SmtpEmailSender smtpEmailSender;

    @Test
    @DisplayName("Should send email successfully and construct MimeMessage properly")
    void shouldSendEmailSuccessfully() {
        // given
        MimeMessage mockMimeMessage = mock(MimeMessage.class);
        given(javaMailSender.createMimeMessage()).willReturn(mockMimeMessage);

        // when
        assertThatCode(() -> smtpEmailSender.sendHtmlEmail(
                "user@example.com",
                "Subject",
                "<p>Body</p>"
        )).doesNotThrowAnyException();

        // then
        then(javaMailSender).should().createMimeMessage();
        then(javaMailSender).should().send(mockMimeMessage);
    }

    @Test
    @DisplayName("Should isolate and swallow MailException preventing asynchronous thread death")
    void shouldSwallowMailException() {
        // given
        MimeMessage mockMimeMessage = mock(MimeMessage.class);
        given(javaMailSender.createMimeMessage()).willReturn(mockMimeMessage);

        willThrow(new MailSendException("SMTP connection timeout"))
                .given(javaMailSender).send(any(MimeMessage.class));

        // when
        assertThatCode(() -> smtpEmailSender.sendHtmlEmail(
                "user@example.com",
                "Subject",
                "<p>Body</p>"
        )).doesNotThrowAnyException();

        // then
        then(javaMailSender).should().send(mockMimeMessage);
    }
}
