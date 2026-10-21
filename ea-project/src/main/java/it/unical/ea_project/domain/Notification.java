package it.unical.ea_project.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "\"NOTIFICATION\"")
public class Notification {

    public enum NotificationType {
        CONFIRMATION,
        BOOKING,
        NOTICE,
        PAYMENT,
        LIST_INVITATION
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    // Usiamo LAZY per performance.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "read", nullable = false)
    private Boolean read = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Riferimento opzionale all'elemento dell'app (es. lista preferita invitata).
    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    @Column(name = "related_entity_type", length = 80)
    private String relatedEntityType;

    // Dati d'azione opzionali, ad esempio il permesso VIEW/EDIT dell'invito.
    @Column(name = "action_data", length = 255)
    private String actionData;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}