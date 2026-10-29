package it.unical.ea_project.repository.payment;

import it.unical.ea_project.domain.payment.SavedCard;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedCardRepository extends JpaRepository<SavedCard, Long> {
    List<SavedCard> findByUserIdOrderByIdDesc(Long id);

    long countByUserId(Long id);

    Optional<SavedCard> findByIdAndUserId(Long id, Long user);
}
