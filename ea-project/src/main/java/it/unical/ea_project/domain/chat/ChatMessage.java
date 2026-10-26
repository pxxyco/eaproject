package it.unical.ea_project.domain.chat;

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
