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
        NotificationDTO notification = notificationService.createNotification(createDTO);
        return ResponseEntity.status(201).body(notification);
    }
    
    @PostMapping("/quick")
    public ResponseEntity<?> createQuickNotification(
            @RequestParam Long userId,
            @RequestParam Notification.NotificationType type,
            @RequestParam String title,
            @RequestParam String message) {
        NotificationDTO notification = notificationService.createNotification(userId, type, title, message);
        return ResponseEntity.status(201).body(notification);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getNotificationsByUserId(@PathVariable Long userId) {
        List<NotificationDTO> notifications = notificationService.getNotificationsByUserId(userId);
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/user/{userId}/paged")
    public ResponseEntity<?> getNotificationsByUserIdPaged(@PathVariable Long userId, Pageable pageable) {
        Page<NotificationDTO> notifications = notificationService.getNotificationsByUserId(userId, pageable);
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<?> getUnreadNotificationsByUserId(@PathVariable Long userId) {
        List<NotificationDTO> notifications = notificationService.getUnreadNotificationsByUserId(userId);
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<?> getUnreadCountByUserId(@PathVariable Long userId) {
        Long count = notificationService.getUnreadCountByUserId(userId);
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getNotificationById(@PathVariable Long id) {
        Optional<NotificationDTO> notification = notificationService.getNotificationById(id);
        if (notification.isPresent()) {
            return ResponseEntity.ok(notification.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        NotificationDTO notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(notification);
    }
    
    @PutMapping("/user/{userId}/mark-all-read")
    public ResponseEntity<?> markAllAsReadByUserId(@PathVariable Long userId) {
        notificationService.markAllAsReadByUserId(userId);
        return ResponseEntity.ok("All notifications marked as read");
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok("Notification deleted successfully");
    }
    
    @DeleteMapping("/user/{userId}/old")
    public ResponseEntity<?> deleteOldNotifications(@PathVariable Long userId, @RequestParam(defaultValue = "30") int daysOld) {
        notificationService.deleteOldNotifications(userId, daysOld);
        return ResponseEntity.ok("Old notifications deleted successfully");
    }
    
    @GetMapping
    public ResponseEntity<?> getAllNotifications() {
        List<NotificationDTO> notifications = notificationService.getAllNotifications();
        return ResponseEntity.ok(notifications);
    }
}
