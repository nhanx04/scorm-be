package com.scorm.generator.repository;

import com.scorm.generator.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Đã sửa 'Userid' thành 'UserId'
    Page<Notification> findByUser_UserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Đã sửa 'Userid' thành 'UserId'
    long countByUser_UserIdAndIsReadFalse(Long userId);

    // Đã sửa 'n.user.userid' thành 'n.user.userId'
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.userId = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") Long userId);

    // Đã sửa 'n.user.userid' thành 'n.user.userId'
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.notificationId = :notificationId AND n.user.userId = :userId")
    int markAsReadByIdAndUserId(@Param("notificationId") Long notificationId, @Param("userId") Long userId);
}