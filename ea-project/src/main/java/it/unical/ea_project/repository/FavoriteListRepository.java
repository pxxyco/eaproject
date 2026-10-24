package it.unical.ea_project.repository;

import it.unical.ea_project.domain.FavoriteList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteListRepository extends JpaRepository<FavoriteList, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select l from FavoriteList l where l.id=:id and l.deleted=false")
    java.util.Optional<FavoriteList> lockActive(@org.springframework.data.repository.query.Param("id") Long id);

    // recupera le liste attive di un utente
    List<FavoriteList> findByUser_IdAndDeletedFalse(Long userId);

    // recupera tutte le liste pubbliche attive
    List<FavoriteList> findByVisibilityAndDeletedFalse(FavoriteList.Visibility visibility);
}