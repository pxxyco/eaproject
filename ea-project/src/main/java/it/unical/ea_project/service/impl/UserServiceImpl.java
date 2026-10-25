package it.unical.ea_project.service.impl;

import it.unical.ea_project.domain.User;
import it.unical.ea_project.repository.UserRepository;
import it.unical.ea_project.security.PasswordPolicy;
import it.unical.ea_project.service.LoginAttemptService;
import it.unical.ea_project.service.PhotoStorageService;
import it.unical.ea_project.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{3,30}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final PhotoStorageService photoStorageService;

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }

    @Override
    public Optional<User> login(String identifier, String password) {
        String normalizedIdentifier = identifier == null ? "" : identifier.trim();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(normalizedIdentifier);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsernameIgnoreCase(normalizedIdentifier);
        }

        if (userOpt.isPresent() && userOpt.get().getHashedPassword() != null
                && password != null && passwordEncoder.matches(password, userOpt.get().getHashedPassword())
                && userOpt.get().getDeletedAt() == null) {
            loginAttemptService.loginSucceeded(normalizedIdentifier);
            return userOpt;
        } else {
            loginAttemptService.loginFailed(normalizedIdentifier);
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public User registerUser(User user) {
        PasswordPolicy.validate(user.getHashedPassword())
                .ifPresent(message -> { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); });

        String username = user.getUsername() == null ? "" : user.getUsername().trim();
        String email = user.getEmail() == null ? "" : user.getEmail().trim();

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lo username deve contenere 3–30 caratteri validi.");
        }
        if (email.length() > 254 || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inserisci un indirizzo email valido.");
        }

        user.setUsername(username);
        user.setEmail(email);

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username già in uso.");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email già registrata.");
        }

        user.setHashedPassword(passwordEncoder.encode(user.getHashedPassword()));
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, String username, String email, String fullName) {
        User user = findActiveUser(userId);
        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedEmail = email == null ? "" : email.trim();
        String normalizedFullName = fullName == null ? "" : fullName.trim().replaceAll("\\s+", " ");

        if (!USERNAME_PATTERN.matcher(normalizedUsername).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Lo username deve contenere 3–30 caratteri: lettere, numeri, punto, trattino o underscore.");
        }
        if (normalizedEmail.length() > 254 || !EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inserisci un indirizzo email valido.");
        }
        if (normalizedFullName.isBlank() || normalizedFullName.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Il nome completo è obbligatorio e non può superare 120 caratteri.");
        }

        userRepository.findByUsernameIgnoreCase(normalizedUsername)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "Username già in uso."); });
        userRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "Email già registrata."); });

        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setFullName(normalizedFullName);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateProfilePhoto(Long userId, MultipartFile file) {
        User user = findActiveUser(userId);
        user.setProfilePhotoUrl(photoStorageService.storeProfilePhoto(userId, file));
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User removeProfilePhoto(Long userId) {
        User user = findActiveUser(userId);
        user.setProfilePhotoUrl(null);
        User saved = userRepository.save(user);
        // Update the entity before filesystem cleanup; on cleanup failure the transaction keeps the previous URL.
        photoStorageService.deleteProfilePhoto(userId);
        return saved;
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = findActiveUser(userId);
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inserisci la password attuale.");
        }
        if (user.isOauthProvider() || user.getHashedPassword() == null || user.getHashedPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Questo account utilizza un accesso esterno e non ha una password locale da modificare.");
        }
        if (!passwordEncoder.matches(currentPassword, user.getHashedPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La password attuale non è corretta.");
        }
        PasswordPolicy.validate(newPassword)
                .ifPresent(message -> { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); });
        if (passwordEncoder.matches(newPassword, user.getHashedPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nuova password deve essere diversa da quella attuale.");
        }
        user.setHashedPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private User findActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente non trovato o non attivo."));
    }
}
