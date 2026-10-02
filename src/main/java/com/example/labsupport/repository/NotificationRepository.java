package com.example.labsupport.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.labsupport.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // The red number on the bell icon
    long countByUserIdAndReadFalse(Long userId);

    // Ownership check: a user can only open or mark their OWN notification
    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    // One UPDATE statement for all unread notifications of a user.
    // The service method calling this must be @Transactional.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId AND n.read = false")
    int markAllAsRead(@Param("userId") Long userId);
}