package it.unical.ea_project.chat;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    @EntityGraph(attributePaths = { "traveler", "organizer" })
    @Query("select c from Conversation c where c.traveler.id=:user or c.organizer.id=:user order by c.updatedAt desc, c.id desc")
    List<Conversation> inbox(@Param("user") Long user, Pageable page);

    Optional<Conversation> findByTravelerIdAndOrganizerId(Long traveler, Long organizer);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conversation c where c.id=:id")
    Optional<Conversation> lock(@Param("id") Long id);

    @org.springframework.transaction.annotation.Transactional
    @Modifying
    @Query(value = "insert into chat_conversation(traveler_id, organizer_id, updated_at) values (:traveler,:organizer,CURRENT_TIMESTAMP) on conflict (traveler_id,organizer_id) do nothing", nativeQuery = true)
    void ensure(@Param("traveler") Long traveler, @Param("organizer") Long organizer);
}
