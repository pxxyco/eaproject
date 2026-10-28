package it.unical.ea_project_javafx.dto.user;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

    private Long id;

    private String username;

    private String email;

    private String fullName;

    private String password;

    private String profilePhotoUrl;

    private String role; // User.Role: ORGANIZER, TRAVELER

    private boolean oauthProvider;

    private String oauthSubject;

    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

}
