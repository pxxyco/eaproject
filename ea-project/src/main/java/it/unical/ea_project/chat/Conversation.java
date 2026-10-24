package it.unical.ea_project.chat;

import it.unical.ea_project.domain.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

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
    private Long travelerReadId;
    private Long organizerReadId;
    @Column(length = 160)
    private String preview;
}
