package com.scorm.generator.service;

import com.scorm.generator.dto.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    // 1. Dùng nội bộ: Bắn một thông báo mới
    void createNotification(Long userId, String title, String message, String type, Long refId, String refType);

    // 2. Phục vụ Frontend: Lấy danh sách thông báo của 1 user (có phân trang)
    Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable);

    // 3. Phục vụ Frontend: Đếm số chuông báo chưa đọc
    long getUnreadCount(Long userId);

    // 4. Phục vụ Frontend: Đánh dấu 1 thông báo là đã đọc
    void markAsRead(Long notificationId, Long userId);

    // 5. Phục vụ Frontend: Đánh dấu TẤT CẢ thông báo là đã đọc
    void markAllAsRead(Long userId);
}