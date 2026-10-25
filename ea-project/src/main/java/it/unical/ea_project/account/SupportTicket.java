package it.unical.ea_project.account;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "support_ticket", indexes = @Index(columnList = "user_id,id"))
@Getter
@Setter
@NoArgsConstructor
public class SupportTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(length = 120, nullable = false)
    private String category;
    @Column(length = 180, nullable = false)
    private String subject;
    @Column(length = 4000, nullable = false)
    private String message;
    private Instant createdAt = Instant.now();
    private String status = "APERTO";
}
