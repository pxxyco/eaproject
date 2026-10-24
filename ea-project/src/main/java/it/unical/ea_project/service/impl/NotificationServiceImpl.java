package it.unical.ea_project.service.impl;

import it.unical.ea_project.domain.Notification;
import it.unical.ea_project.domain.User;
import it.unical.ea_project.repository.NotificationRepository;
import it.unical.ea_project.service.NotificationService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public List<Notification> getAllNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Override
    public Page<Notification> getNotificationsPaginated(User user, int page, int size) {
        return notificationRepository.findByUser(user, PageRequest.of(page, size));
    }

    @Override
    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndReadOrderByCreatedAtDesc(user, false);
    }

    @Override
    public long countUnread(User user) {
        return notificationRepository.countByUserAndReadFalse(user);
    }

    @Override
    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsReadForUser(user);
    }

    @Override
    @Transactional
    public Notification createNotification(Notification notification) {
        return notificationRepository.save(notification);
    }

    @Override
    public Optional<Notification> getNotificationForUser(Long notificationId, Long userId) {
        return notificationRepository.findByNotificationIdAndUser_Id(notificationId, userId);
    }

    @Override
    @Transactional
    public void markNotificationAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByNotificationIdAndUser_Id(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Notifica non trovata."));
        notification.setRead(true);
    }

    @Override
    public boolean hasPendingListInvitation(Long userId, Long listId) {
        return notificationRepository.existsByUser_IdAndTypeAndRelatedEntityId(
                userId, Notification.NotificationType.LIST_INVITATION, listId);
    }

    @Override
    @Transactional
    public void resolveListInvitation(Long notificationId, Long userId, String title, String message) {
        Notification notification = notificationRepository.findByNotificationIdAndUser_Id(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Notifica non trovata."));
        notification.setRead(true);
        notification.setType(Notification.NotificationType.NOTICE);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedEntityId(null);
        notification.setRelatedEntityType(null);
        notification.setActionData(null);
    }

}