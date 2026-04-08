package com.scorm.generator.dto;

import lombok.Builder;
import lombok.Data;
import java.time.ZonedDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long notificationId;
    private String title;
    private String message;
    private String type;
    private Boolean isRead;
    private Long referenceId;
    private String referenceType;
    private ZonedDateTime createdAt;
}