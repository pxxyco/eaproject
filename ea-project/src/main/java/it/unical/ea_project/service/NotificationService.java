package it.unical.ea_project.service;

import it.unical.ea_project.domain.Notification;
import it.unical.ea_project.domain.User;
import org.springframework.data.domain.Page;
import java.util.List;
import java.util.Optional;

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