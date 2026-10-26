package it.unical.ea_project.service.favorites;

import it.unical.ea_project.domain.favorites.ListSharing;
import java.util.List;

public interface ListSharingService {

    ListSharing shareList(Long listId, Long userId, ListSharing.SharingPermission permission);

    List<ListSharing> getSharingsByListId(Long listId);

    List<ListSharing> getSharedListsForUser(Long userId);

    void updatePermission(Long listId, Long userId, ListSharing.SharingPermission newPermission);

    void revokeSharing(Long listId, Long userId);

}
