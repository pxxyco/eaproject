package it.unical.ea_project_javafx.security;

import it.unical.ea_project_javafx.dto.UserDTO;
import lombok.Getter;

@Getter
public class UserSession {

    private Long id;
    
    private String username;
    private String email;
    private String fullName;
    private String profilePhotoUrl;
    private String role;    
    private String jwtToken;
    private String accessToken;
    private String refreshToken;
    
    private boolean oauthProvider;
    private boolean loggedIn = false;
    
    private static UserSession instance;

    private UserSession() {
    }

    public static synchronized UserSession getInstance() {

        if (instance == null) {
            instance = new UserSession();
        }

        return instance;

    }

    public void setSession(UserDTO userDTO) {

        if (userDTO != null) {
            this.id = userDTO.getId();
            this.username = userDTO.getUsername();
            this.email = userDTO.getEmail();
            this.fullName = userDTO.getFullName();
            this.profilePhotoUrl = userDTO.getProfilePhotoUrl();
            this.role = userDTO.getRole();
            this.oauthProvider = userDTO.isOauthProvider();
            this.loggedIn = true;
        }

    }

    public void clear() {

        this.id = null;
        this.username = null;
        this.email = null;
        this.fullName = null;
        this.profilePhotoUrl = null;
        this.role = null;
        this.oauthProvider = false;
        this.jwtToken = null;
        this.accessToken = null;
        this.refreshToken = null;
        this.loggedIn = false;

    }

    public void setTokens(String access, String refresh) {

        this.accessToken = access;
        this.jwtToken = access;
        this.refreshToken = refresh;

    }
}