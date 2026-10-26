package it.unical.ea_project.service.notification;

import it.unical.ea_project.domain.notification.Notification;
import it.unical.ea_project.domain.user.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface NotificationService {

    List<Notification> getAllNotifications(User user);

    Page<Notification> getNotificationsPaginated(User user, int page, int size);

    List<Notification> getUnreadNotifications(User user);

    long countUnread(User user);

    void markAllAsRead(User user);

    Notification createNotification(Notification notification);

    Optional<Notification> getNotificationForUser(Long notificationId, Long userId);

    void markNotificationAsRead(Long notificationId, Long userId);

    boolean hasPendingListInvitation(Long userId, Long listId);

    void resolveListInvitation(Long notificationId, Long userId, String title, String message);

}
