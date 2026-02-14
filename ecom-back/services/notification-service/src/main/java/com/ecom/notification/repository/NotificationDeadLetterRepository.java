package com.ecom.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.notification.entity.NotificationDeadLetterRecord;

public interface NotificationDeadLetterRepository extends JpaRepository<NotificationDeadLetterRecord, Long> {

    Optional<NotificationDeadLetterRecord> findByEventId(String eventId);

    List<NotificationDeadLetterRecord> findTop100ByOrderByCreatedAtDesc();
}
