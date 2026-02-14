package com.ecom.notification.service;

public interface EmailSender {

    void send(String recipient, String subject, String body);
}
