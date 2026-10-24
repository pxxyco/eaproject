package it.unical.ea_project.repository;

import it.unical.ea_project.domain.Notification;
import it.unical.ea_project.domain.Notification.NotificationType;
import it.unical.ea_project.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByNotificationIdAndUser_Id(Long notificationId, Long userId);

    boolean existsByUser_IdAndTypeAndRelatedEntityId(
            Long userId, NotificationType type, Long relatedEntityId);

    @Modifying
    @Query("UPDATE Notification n SET n.type = :resolvedType, n.read = true, n.relatedEntityId = null, n.relatedEntityType = null, n.actionData = null, n.title = 'Invito non più disponibile', n.message = 'La lista non è più condivisa.' WHERE n.relatedEntityId = :listId AND n.type = :invitationType")
    int invalidateListInvitations(Long listId, NotificationType invitationType, NotificationType resolvedType);

    // Recupera le notifiche di un utente ordinate dalla più recente (decrescente per data)
    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    // Recupera le notifiche con supporto alla paginazione (utile per non rallentare l'app se l'utente ne ha centinaia)
    Page<Notification> findByUser(User user, Pageable pageable);

    // Trova le notifiche filtrate per stato di lettura (es. solo quelle non lette: read = false)
    List<Notification> findByUserAndReadOrderByCreatedAtDesc(User user, Boolean read);

    // Conta quante notifiche NON lette ha un utente
    long countByUserAndReadFalse(User user);

    // Trova le notifiche di un utente per una categoria specifica (es. solo quelle di PAYMENTS)
    List<Notification> findByUserAndTypeOrderByCreatedAtDesc(User user, NotificationType type);

    // Segna come lette tutte le notifiche di un determinato utente in un'unica operazione
    @Transactional
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user = :user AND n.read = false")
    int markAllAsReadForUser(User user);
}