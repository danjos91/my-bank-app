package io.github.danjos.mybankapp.notifications.service;

import io.github.danjos.mybankapp.notifications.dto.CreateNotificationDTO;
import io.github.danjos.mybankapp.notifications.dto.NotificationDTO;
import io.github.danjos.mybankapp.notifications.entity.Notification;
import io.github.danjos.mybankapp.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    
    public NotificationDTO createNotification(CreateNotificationDTO createDTO) {
        Notification notification = Notification.builder()
                .userId(createDTO.getUserId())
                .notificationType(createDTO.getNotificationType())
                .title(createDTO.getTitle())
                .message(createDTO.getMessage())
                .isRead(false)
                .build();
        
        Notification savedNotification = notificationRepository.save(notification);
        return convertToDTO(savedNotification);
    }
    
    public NotificationDTO createNotification(Long userId, Notification.NotificationType type, String title, String message) {
        CreateNotificationDTO createDTO = CreateNotificationDTO.builder()
                .userId(userId)
                .notificationType(type)
                .title(title)
                .message(message)
                .build();
        
        return createNotification(createDTO);
    }
    
    @Transactional(readOnly = true)
    public List<NotificationDTO> getNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotificationsByUserId(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::convertToDTO);
    }
    
    @Transactional(readOnly = true)
    public List<NotificationDTO> getUnreadNotificationsByUserId(Long userId) {
        return notificationRepository.findUnreadByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Long getUnreadCountByUserId(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }
    
    @Transactional(readOnly = true)
    public Optional<NotificationDTO> getNotificationById(Long id) {
        return notificationRepository.findById(id)
                .map(this::convertToDTO);
    }
    
    public NotificationDTO markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        
        notification.markAsRead();
        Notification savedNotification = notificationRepository.save(notification);
        return convertToDTO(savedNotification);
    }
    
    public void markAllAsReadByUserId(Long userId) {
        List<Notification> unreadNotifications = notificationRepository.findUnreadByUserIdOrderByCreatedAtDesc(userId);
        unreadNotifications.forEach(Notification::markAsRead);
        notificationRepository.saveAll(unreadNotifications);
    }
    
    public void deleteNotification(Long id) {
        if (!notificationRepository.existsById(id)) {
            throw new IllegalArgumentException("Notification not found");
        }
        notificationRepository.deleteById(id);
    }
    
    public void deleteOldNotifications(Long userId, int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        notificationRepository.deleteByUserIdAndCreatedAtBefore(userId, cutoffDate);
    }
    
    @Transactional(readOnly = true)
    public List<NotificationDTO> getAllNotifications() {
        return notificationRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    private NotificationDTO convertToDTO(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .notificationType(notification.getNotificationType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
