package io.github.danjos.mybankapp.notifications.controller;

import io.github.danjos.mybankapp.notifications.dto.CreateNotificationDTO;
import io.github.danjos.mybankapp.notifications.dto.NotificationDTO;
import io.github.danjos.mybankapp.notifications.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationDTO> createNotification(@Valid @RequestBody CreateNotificationDTO request) {
        NotificationDTO created = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationDTO>> getNotificationsByUser(@PathVariable Long userId) {
        List<NotificationDTO> notifications = notificationService.getNotificationsByUserId(userId);
        return ResponseEntity.ok(notifications);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationDTO> markAsRead(@PathVariable Long id) {
        NotificationDTO updated = notificationService.markAsRead(id);
        return ResponseEntity.ok(updated);
    }
}

