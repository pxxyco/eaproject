package it.unical.ea_project.account;

import java.util.*;

public interface SavedCardRepository extends org.springframework.data.jpa.repository.JpaRepository<SavedCard, Long> {
    List<SavedCard> findByUserIdOrderByIdDesc(Long id);

    long countByUserId(Long id);

    Optional<SavedCard> findByIdAndUserId(Long id, Long user);
}
