package it.unical.ea_project.repository.favorites;

import it.unical.ea_project.domain.favorites.FavoriteList;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteListRepository extends JpaRepository<FavoriteList, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from FavoriteList l where l.id=:id and l.deleted=false")
    Optional<FavoriteList> lockActive(@Param("id") Long id);

    // recupera le liste attive di un utente
    List<FavoriteList> findByUser_IdAndDeletedFalse(Long userId);

    // recupera tutte le liste pubbliche attive

}
