package io.github.danjos.mybankapp.transfer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNotificationDTO {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Notification type is required")
    @Size(max = 50, message = "Notification type must not exceed 50 characters")
    private String type;
    
    @NotNull(message = "Title is required")
    @Size(max = 100, message = "Title must not exceed 100 characters")
    private String title;
    
    @NotNull(message = "Message is required")
    @Size(max = 500, message = "Message must not exceed 500 characters")
    private String message;
    
    @Builder.Default
    private boolean isRead = false;
}
