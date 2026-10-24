package it.unical.ea_project.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import it.unical.ea_project.domain.User;
import it.unical.ea_project.dto.UserDTO;
import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.JwtService;
import it.unical.ea_project.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public record RegistrationRequest(String username, String email, String fullName, String password, String role) {}
    public record LoginRequest(String identifier, String password) {}
    public record UpdateProfileRequest(String username, String email, String fullName) {}
    public record ChangePasswordRequest(String currentPassword, String newPassword) {}

    @GetMapping("/exists")
    public ResponseEntity<Void> emailExists(@RequestParam String email) {
        return userService.existsByEmail(email.trim()) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<UserDTO> registerUser(@RequestBody RegistrationRequest dto) {
        User.Role role;
        try {
            role = User.Role.valueOf(dto.role());
        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.badRequest().build();
        }
        if (role != User.Role.TRAVELER && role != User.Role.ORGANIZER) {
            return ResponseEntity.badRequest().build();
        }

        User user = new User();
        user.setUsername(dto.username());
        user.setEmail(dto.email());
        user.setFullName(dto.fullName());
        user.setRole(role);
        user.setOauthProvider(false);
        user.setOauthSubject(null);
        user.setHashedPassword(dto.password());
        User saved = userService.registerUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(saved));
    }

    @PostMapping("/login")
    public ResponseEntity<UserDTO> login(@RequestBody LoginRequest req) {
        if (req == null || req.identifier() == null || req.password() == null) {
            return ResponseEntity.badRequest().build();
        }
        Optional<User> userOpt = userService.login(req.identifier(), req.password());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userOpt.get();
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateAccessToken(user))
                .header("X-Refresh-Token", jwtService.generateRefreshToken(user))
                .body(convertToDto(user));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(@RequestHeader("X-Refresh-Token") String refreshToken) {
        try {
            Claims c = jwtService.parseToken(refreshToken, JwtService.TYPE_REFRESH);
            Long id = ((Number) c.get("id")).longValue();
            return userService.getUserById(id)
                    .filter(u -> u.getDeletedAt() == null)
                    .map(u -> ResponseEntity.ok()
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateAccessToken(u))
                            .<Void>build())
                    .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        } catch (JwtException | IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> me(@AuthenticationPrincipal AuthUser auth) {
        return userService.getUserById(auth.id())
                .filter(u -> u.getDeletedAt() == null)
                .map(u -> ResponseEntity.ok(convertToDto(u)))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PutMapping("/me")
    public ResponseEntity<UserDTO> updateMyProfile(
            @AuthenticationPrincipal AuthUser auth,
            @RequestBody UpdateProfileRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }
        User updated = userService.updateProfile(auth.id(), request.username(), request.email(), request.fullName());
        return ResponseEntity.ok(convertToDto(updated));
    }

    @PostMapping("/me/profile-photo")
    public ResponseEntity<UserDTO> uploadMyProfilePhoto(
            @AuthenticationPrincipal AuthUser auth,
            @RequestParam("file") MultipartFile file) {
        User updated = userService.updateProfilePhoto(auth.id(), file);
        return ResponseEntity.ok(convertToDto(updated));
    }

    @DeleteMapping("/me/profile-photo")
    public ResponseEntity<UserDTO> deleteMyProfilePhoto(@AuthenticationPrincipal AuthUser auth) {
        User updated = userService.removeProfilePhoto(auth.id());
        return ResponseEntity.ok(convertToDto(updated));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal AuthUser auth,
            @RequestBody ChangePasswordRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }
        userService.changePassword(auth.id(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    private UserDTO convertToDto(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .profilePhotoUrl(user.getProfilePhotoUrl())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .oauthProvider(user.isOauthProvider())
                .createdAt(user.getCreatedAt())
                .build();
    }
}