package it.unical.ea_project.repository.notification;

import it.unical.ea_project.domain.notification.Notification.NotificationType;
import it.unical.ea_project.domain.notification.Notification;
import it.unical.ea_project.domain.user.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

        Optional<Notification> findFirstByUser_IdAndRelatedEntityTypeAndRelatedEntityIdAndReadFalse(Long user,
                        String type,
                        Long entity);

        @Modifying
        @Query("update Notification n set n.read=true where n.user.id=:user and n.relatedEntityType='CHAT' and n.relatedEntityId=:conversation and n.read=false")
        void readConversation(Long user, Long conversation);

        Optional<Notification> findByNotificationIdAndUser_Id(Long notificationId, Long userId);

        boolean existsByUser_IdAndTypeAndRelatedEntityId(
                        Long userId, NotificationType type, Long relatedEntityId);

        @Modifying
        @Query("UPDATE Notification n SET n.type = :resolvedType, n.read = true, n.relatedEntityId = null, n.relatedEntityType = null, n.actionData = null, n.title = 'Invito non più disponibile', n.message = 'La lista non è più condivisa.' WHERE n.relatedEntityId = :listId AND n.type = :invitationType")
        int invalidateListInvitations(Long listId, NotificationType invitationType, NotificationType resolvedType);

        // Recupera le notifiche con supporto alla paginazione (utile per non rallentare
        // l'app se l'utente ne ha centinaia)
        Page<Notification> findByUser(User user, Pageable pageable);

        // Trova le notifiche filtrate per stato di lettura (es. solo quelle non lette:
        // read = false)
        List<Notification> findByUserAndReadOrderByCreatedAtDesc(User user, Boolean read);

        List<Notification> findByUserAndReadOrderByCreatedAtDesc(User user, Boolean read, Pageable page);

        // Conta quante notifiche NON lette ha un utente
        long countByUserAndReadFalse(User user);

        // Trova le notifiche di un utente per una categoria specifica (es. solo quelle
        // di PAYMENTS)
        List<Notification> findByUserAndTypeOrderByCreatedAtDesc(User user, NotificationType type);

        // Segna come lette tutte le notifiche di un determinato utente in un'unica
        // operazione
        @Transactional
        @Modifying
        @Query("UPDATE Notification n SET n.read = true WHERE n.user = :user AND n.read = false")
        int markAllAsReadForUser(User user);
}
