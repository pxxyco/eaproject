package it.unical.ea_project.domain.chat;

import it.unical.ea_project.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_conversation", indexes = {
        @Index(name = "chat_traveler_inbox", columnList = "traveler_id,updated_at"),
        @Index(name = "chat_organizer_inbox", columnList = "organizer_id,updated_at") }, uniqueConstraints = @UniqueConstraint(columnNames = {
                "traveler_id", "organizer_id" }))
@Getter
@Setter
@NoArgsConstructor
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "traveler_id")
    private User traveler;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizer_id")
    private User organizer;
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
    private Instant travelerDeletedAt;
    private Instant organizerDeletedAt;
    private Long travelerClearedThroughId;
    private Long organizerClearedThroughId;
    private Long travelerReadId;
    private Long organizerReadId;
    @Column(length = 160)
    private String preview;
}
