package it.unical.ea_project.domain.support;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
