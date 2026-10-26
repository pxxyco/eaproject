package it.unical.ea_project.repository.chat;

import it.unical.ea_project.domain.chat.Conversation;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    @EntityGraph(attributePaths = { "traveler", "organizer" })
    @Query("select c from Conversation c where (c.traveler.id=:user and c.travelerDeletedAt is null) or (c.organizer.id=:user and c.organizerDeletedAt is null) order by c.updatedAt desc, c.id desc")
    List<Conversation> inbox(@Param("user") Long user, Pageable page);

    Optional<Conversation> findByTravelerIdAndOrganizerId(Long traveler, Long organizer);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conversation c where c.id=:id")
    Optional<Conversation> lock(@Param("id") Long id);

}
