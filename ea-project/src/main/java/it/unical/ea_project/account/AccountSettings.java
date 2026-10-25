package it.unical.ea_project.account;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "account_settings")
@Getter
@Setter
@NoArgsConstructor
public class AccountSettings {
    @Id
    @Column(name = "user_id")
    private Long userId;
    private boolean chatAlerts = true;
    private boolean bookingAlerts = true;
    private boolean personalized = true;
    @Column(length = 120)
    private String brand;
    @Column(length = 2000)
    private String bio;
    @Column(length = 120)
    private String payoutHolder;
    @Column(length = 34)
    private String iban;
    @Column(length = 11)
    private String bic;

    public AccountSettings(Long id) {
        userId = id;
    }
}
