package com.ecom.notification.service;

import java.util.List;
import java.util.Map;

import com.ecom.common.DomainEvent;
import com.ecom.notification.dto.NotificationDeadLetterResponse;
import com.ecom.notification.dto.NotificationResponse;

public interface NotificationUseCases {

    void handleDomainEvent(DomainEvent<Map<String, Object>> event, String fallbackEventType);

    List<NotificationResponse> listByUser(Long userId);

    List<NotificationResponse> listFailed();

    int retryFailed();

    List<NotificationDeadLetterResponse> listDeadLetters();

    boolean requeueDeadLetter(Long deadLetterId);
}
