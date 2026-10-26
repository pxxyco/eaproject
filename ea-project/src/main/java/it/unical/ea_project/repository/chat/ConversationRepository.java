package it.unical.ea_project.repository.chat;

import it.unical.ea_project.domain.chat.Conversation;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    @EntityGraph(attributePaths = { "traveler", "organizer" })
    @Query("select c from Conversation c where c.traveler.id=:user or c.organizer.id=:user order by c.updatedAt desc, c.id desc")
    List<Conversation> inbox(@Param("user") Long user, Pageable page);

    Optional<Conversation> findByTravelerIdAndOrganizerId(Long traveler, Long organizer);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conversation c where c.id=:id")
    Optional<Conversation> lock(@Param("id") Long id);

    @Transactional
    @Modifying
    @Query(value = "insert into chat_conversation(traveler_id, organizer_id, updated_at) values (:traveler,:organizer,CURRENT_TIMESTAMP) on conflict (traveler_id,organizer_id) do nothing", nativeQuery = true)
    void ensure(@Param("traveler") Long traveler, @Param("organizer") Long organizer);
}
