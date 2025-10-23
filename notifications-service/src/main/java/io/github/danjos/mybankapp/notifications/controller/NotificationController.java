package io.github.danjos.mybankapp.notifications.controller;

import io.github.danjos.mybankapp.notifications.dto.CreateNotificationDTO;
import io.github.danjos.mybankapp.notifications.dto.NotificationDTO;
import io.github.danjos.mybankapp.notifications.entity.Notification;
import io.github.danjos.mybankapp.notifications.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    
    @PostMapping
    public ResponseEntity<?> createNotification(@Valid @RequestBody CreateNotificationDTO createDTO) {
        try {
            NotificationDTO notification = notificationService.createNotification(createDTO);
            return ResponseEntity.status(201).body(notification);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating notification: " + e.getMessage());
        }
    }
    
    @PostMapping("/quick")
    public ResponseEntity<?> createQuickNotification(
            @RequestParam Long userId,
            @RequestParam Notification.NotificationType type,
            @RequestParam String title,
            @RequestParam String message) {
        try {
            NotificationDTO notification = notificationService.createNotification(userId, type, title, message);
            return ResponseEntity.status(201).body(notification);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating notification: " + e.getMessage());
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getNotificationsByUserId(@PathVariable Long userId) {
        try {
            List<NotificationDTO> notifications = notificationService.getNotificationsByUserId(userId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving notifications: " + e.getMessage());
        }
    }
    
    @GetMapping("/user/{userId}/paged")
    public ResponseEntity<?> getNotificationsByUserIdPaged(@PathVariable Long userId, Pageable pageable) {
        try {
            Page<NotificationDTO> notifications = notificationService.getNotificationsByUserId(userId, pageable);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving notifications: " + e.getMessage());
        }
    }
    
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<?> getUnreadNotificationsByUserId(@PathVariable Long userId) {
        try {
            List<NotificationDTO> notifications = notificationService.getUnreadNotificationsByUserId(userId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving unread notifications: " + e.getMessage());
        }
    }
    
    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<?> getUnreadCountByUserId(@PathVariable Long userId) {
        try {
            Long count = notificationService.getUnreadCountByUserId(userId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving unread count: " + e.getMessage());
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getNotificationById(@PathVariable Long id) {
        try {
            Optional<NotificationDTO> notification = notificationService.getNotificationById(id);
            if (notification.isPresent()) {
                return ResponseEntity.ok(notification.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving notification: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        try {
            NotificationDTO notification = notificationService.markAsRead(id);
            return ResponseEntity.ok(notification);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error marking notification as read: " + e.getMessage());
        }
    }
    
    @PutMapping("/user/{userId}/mark-all-read")
    public ResponseEntity<?> markAllAsReadByUserId(@PathVariable Long userId) {
        try {
            notificationService.markAllAsReadByUserId(userId);
            return ResponseEntity.ok("All notifications marked as read");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error marking notifications as read: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id) {
        try {
            notificationService.deleteNotification(id);
            return ResponseEntity.ok("Notification deleted successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting notification: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/user/{userId}/old")
    public ResponseEntity<?> deleteOldNotifications(@PathVariable Long userId, @RequestParam(defaultValue = "30") int daysOld) {
        try {
            notificationService.deleteOldNotifications(userId, daysOld);
            return ResponseEntity.ok("Old notifications deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting old notifications: " + e.getMessage());
        }
    }
    
    @GetMapping
    public ResponseEntity<?> getAllNotifications() {
        try {
            List<NotificationDTO> notifications = notificationService.getAllNotifications();
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving notifications: " + e.getMessage());
        }
    }
}
