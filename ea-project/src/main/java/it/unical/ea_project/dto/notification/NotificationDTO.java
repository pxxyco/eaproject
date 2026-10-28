package it.unical.ea_project.dto.notification;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {

    private Long notificationId;

    private Long userId;

    // Notification.NotificationType: CONFIRMATION, BOOKING, NOTICE, PAYMENT
    private String type;

    private String title;

    private String message;

    private Boolean read;

    private LocalDateTime createdAt;

    private Long relatedEntityId;

    private String relatedEntityType;

    private String actionData;
}
