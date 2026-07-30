package com.autotrade.dashboard.notification;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Notification> findAllByReadAtIsNull();

    long countByReadAtIsNull();
}
