package it.unical.ea_project.chat;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "chat_message", indexes = @Index(name = "chat_message_history", columnList = "conversation_id,id"), uniqueConstraints = @UniqueConstraint(columnNames = {
        "conversation_id", "sender_id", "client_id" }))
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;
    @Column(name = "sender_id", nullable = false)
    private Long senderId;
    @Column(name = "client_id", nullable = false, length = 36)
    private String clientId;
    @Column(nullable = false, length = 4000)
    private String body;
    @Column(nullable = false)
    private Instant sentAt = Instant.now();
}
