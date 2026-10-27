package it.unical.ea_project.repository.chat;

import it.unical.ea_project.domain.chat.ChatMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @EntityGraph(attributePaths = { "replyTo" })
    @Query("select m from ChatMessage m where m.conversation.id=:conversation and m.id>:cleared and (:before is null or m.id<:before) order by m.id desc")
    List<ChatMessage> history(@Param("conversation") Long conversation, @Param("before") Long before,
            @Param("cleared") Long cleared,
            Pageable pageable);

    Optional<ChatMessage> findByConversationIdAndSenderIdAndClientId(Long conversation, Long sender, String clientId);

    @Query("select m.conversation.id,count(m) from ChatMessage m join m.conversation c where c.id in :conversations and m.senderId<>:user and ((c.traveler.id=:user and m.id>coalesce(c.travelerReadId,0)) or (c.organizer.id=:user and m.id>coalesce(c.organizerReadId,0))) group by m.conversation.id")
    List<Object[]> unread(@Param("user") Long user, @Param("conversations") List<Long> conversations);

    @Query("select max(m.id) from ChatMessage m where m.conversation.id=:conversation")
    Long latest(@Param("conversation") Long conversation);

    boolean existsByIdAndConversationId(Long id, Long conversation);
}
