package it.unical.ea_project.repository.favorites;

import it.unical.ea_project.domain.favorites.ListSharing;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListSharingRepository extends JpaRepository<ListSharing, ListSharing.ListSharingId> {

    // trova tutte le condivisioni attive per una specifica lista
    @EntityGraph(attributePaths = { "user", "favoriteList" })
    List<ListSharing> findByFavoriteListId(Long listId);

    // trova tutte le liste condivise con uno specifico utente
    @EntityGraph(attributePaths = { "user", "favoriteList" })
    List<ListSharing> findByUser_Id(Long userId);

    boolean existsByFavoriteList_IdAndUser_IdAndPermission(Long listId, Long userId,
            ListSharing.SharingPermission permission);

    // verifica se una lista è già stata condivisa con un determinato utente
    boolean existsByFavoriteList_IdAndUser_Id(Long favoriteListId, Long userId);
}
