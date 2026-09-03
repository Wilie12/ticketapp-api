package com.nn.ticketapp_api.notification.service;

public interface EmailSender {
    void sendHtmlEmail(String to, String subject, String htmlBody);
}
