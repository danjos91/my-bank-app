package io.github.danjos.mybankapp.accounts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNotificationDTO {
    private Long userId;
    private String type;
    private String title;
    private String message;
    private String notificationType; // Alternative field name if service expects it
}

