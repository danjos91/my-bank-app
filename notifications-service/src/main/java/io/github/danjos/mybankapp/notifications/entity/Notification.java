package io.github.danjos.mybankapp.notifications.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications_log", schema = "notifications_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    @NotNull(message = "Notification type is required")
    private NotificationType notificationType;
    
    @Column(name = "title", nullable = false, length = 200)
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;
    
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Message is required")
    private String message;
    
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    // Business methods
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
    
    public boolean isUnread() {
        return !isRead;
    }
    
    public String getFormattedCreatedAt() {
        return createdAt.toString();
    }
    
    public enum NotificationType {
        ACCOUNT_CREATED,
        ACCOUNT_UPDATED,
        ACCOUNT_DELETED,
        DEPOSIT_SUCCESS,
        DEPOSIT_FAILED,
        WITHDRAWAL_SUCCESS,
        WITHDRAWAL_FAILED,
        TRANSFER_SENT,
        TRANSFER_RECEIVED,
        TRANSFER_FAILED,
        PASSWORD_CHANGED,
        PROFILE_UPDATED,
        BALANCE_LOW,
        SUSPICIOUS_ACTIVITY
    }
}
