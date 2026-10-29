package it.unical.ea_project.domain.favorites;

import it.unical.ea_project.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "favoriteList", "user" })
@Entity
@IdClass(ListSharing.ListSharingId.class)
@Table(name = "\"LIST_SHARING\"")
public class ListSharing {

    public enum SharingPermission {

        VIEW,
        EDIT

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListSharingId implements Serializable {
        private Long favoriteList;
        private Long user;
    }

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false)
    private FavoriteList favoriteList;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false)
    private SharingPermission permission;

    @Column(name = "shared_at")
    private LocalDateTime sharedAt;
}
