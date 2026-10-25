package it.unical.ea_project.account;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "saved_card", indexes = @Index(columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
public class SavedCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(length = 20, nullable = false)
    private String brand;
    @Column(length = 4, nullable = false)
    private String last4;
    @Column(length = 120, nullable = false)
    private String holder;
    private int expiryMonth;
    private int expiryYear;
}
