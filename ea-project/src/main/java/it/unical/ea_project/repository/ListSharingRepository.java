package it.unical.ea_project.repository;

import it.unical.ea_project.domain.ListSharing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

@Repository
public interface ListSharingRepository extends JpaRepository<ListSharing, ListSharing.ListSharingId> {

    //trova tutte le condivisioni attive per una specifica lista
    @EntityGraph(attributePaths = {"user", "favoriteList"})
    List<ListSharing> findByFavoriteListId(Long listId);

    //trova tutte le liste condivise con uno specifico utente
    @EntityGraph(attributePaths = {"user", "favoriteList"})
    List<ListSharing> findByUser_Id(Long userId);

    //verifica se una lista è già stata condivisa con un determinato utente
    boolean existsByFavoriteList_IdAndUser_Id(Long favoriteListId, Long userId);
}