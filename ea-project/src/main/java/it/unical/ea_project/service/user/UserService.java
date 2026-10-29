package it.unical.ea_project.service.user;

import it.unical.ea_project.domain.user.User;
import java.util.List;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    List<User> getAllUsers();

    Optional<User> getUserById(Long id);

    boolean existsByEmail(String email);

    Optional<User> login(String identifier, String password);

    User registerUser(User user);

    User updateProfile(Long userId, String username, String email, String fullName);

    User updateProfilePhoto(Long userId, MultipartFile file);

    User removeProfilePhoto(Long userId);

    void changePassword(Long userId, String currentPassword, String newPassword);

}
