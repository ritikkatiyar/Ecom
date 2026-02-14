package com.ecom.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.notification.entity.NotificationRecord;
import com.ecom.notification.entity.NotificationStatus;

public interface NotificationRecordRepository extends JpaRepository<NotificationRecord, Long> {

    Optional<NotificationRecord> findByEventId(String eventId);

    List<NotificationRecord> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<NotificationRecord> findTop100ByStatusOrderByCreatedAtAsc(NotificationStatus status);
}
