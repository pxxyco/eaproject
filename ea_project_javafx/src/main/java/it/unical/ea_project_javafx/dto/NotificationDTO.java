package it.unical.ea_project_javafx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {

    private Long notificationId;

    private Long userId;

    private String type;    // Notification.NotificationType: CONFIRMATION, BOOKING, NOTICE, PAYMENT

    private String title;

    private String message;

    private Boolean read;

    private LocalDateTime createdAt;

}
