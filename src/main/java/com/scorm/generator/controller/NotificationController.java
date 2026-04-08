package com.scorm.generator.controller;

import com.scorm.generator.dto.NotificationResponse;
import com.scorm.generator.entity.User;
import com.scorm.generator.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 1. Lấy danh sách thông báo của người dùng đang đăng nhập (có phân trang)
     * Frontend gọi: GET /api/notifications?page=0&size=10
     */
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getUserNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> notifications = notificationService.getUserNotifications(user.getUserId(), pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 2. Lấy số lượng thông báo chưa đọc (để hiển thị số chấm đỏ trên quả chuông)
     * Frontend gọi: GET /api/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal User user) {
        long count = notificationService.getUnreadCount(user.getUserId());
        return ResponseEntity.ok(count);
    }

    /**
     * 3. Đánh dấu một thông báo cụ thể là đã đọc
     * Frontend gọi: PATCH /api/notifications/{id}/read
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal User user) {

        notificationService.markAsRead(notificationId, user.getUserId());
        return ResponseEntity.ok().build();
    }

    /**
     * 4. Đánh dấu TẤT CẢ thông báo là đã đọc
     * Frontend gọi: PATCH /api/notifications/read-all
     */
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal User user) {
        notificationService.markAllAsRead(user.getUserId());
        return ResponseEntity.ok().build();
    }
}