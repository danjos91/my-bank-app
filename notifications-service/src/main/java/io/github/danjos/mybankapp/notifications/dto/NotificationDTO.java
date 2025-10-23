package io.github.danjos.mybankapp.notifications.dto;

import io.github.danjos.mybankapp.notifications.entity.Notification;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {
    
    private Long id;
    private Long userId;
    private Notification.NotificationType notificationType;
    private String title;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    
    // Business methods
    public boolean isUnread() {
        return !isRead;
    }
    
    public String getFormattedCreatedAt() {
        return createdAt.toString();
    }
    
    public String getFormattedReadAt() {
        return readAt != null ? readAt.toString() : null;
    }
}
