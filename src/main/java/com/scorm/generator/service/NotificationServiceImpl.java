package com.scorm.generator.service;

import com.scorm.generator.dto.NotificationResponse;
import com.scorm.generator.entity.Notification;
import com.scorm.generator.entity.User;
import com.scorm.generator.exception.AppException;
import com.scorm.generator.repository.NotificationRepository;
import com.scorm.generator.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void createNotification(Long userId, String title, String message, String type, Long refId, String refType) {
        User user = userRepository.getReferenceById(userId);

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .referenceId(refId)
                .referenceType(refType)
                .build();

        notificationRepository.save(notification);
        log.info("Đã tạo thông báo mới cho User ID: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable) {
        // Đã cập nhật tên hàm mới (UserId)
        Page<Notification> notificationPage = notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(userId,
                pageable);
        return notificationPage.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        // Đã cập nhật tên hàm mới (UserId)
        return notificationRepository.countByUser_UserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        int updatedRows = notificationRepository.markAsReadByIdAndUserId(notificationId, userId);
        if (updatedRows == 0) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo hoặc bạn không có quyền truy cập");
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .isRead(notification.getIsRead())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}