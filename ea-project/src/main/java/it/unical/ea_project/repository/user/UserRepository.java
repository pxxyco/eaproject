package it.unical.ea_project.repository.user;

import it.unical.ea_project.domain.user.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<User, Long> {
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select u from User u where u.id=:id")
        Optional<User> lockForChat(@Param("id") Long id);

        @Modifying
        @Transactional
        @Query("update User u set u.authVersion = u.authVersion + 1, u.rowVersion = u.rowVersion + 1 where u.id = :id")
        void revokeSessions(@Param("id") Long id);

        Optional<User> findByEmailIgnoreCase(String email);

        Optional<User> findByUsernameIgnoreCase(String username);

        boolean existsByEmailIgnoreCase(String email);

        boolean existsByUsernameIgnoreCase(String username);

        @Query("select u from User u where u.deletedAt is null and u.id <> :currentUserId " +
                        "and (lower(u.username) like lower(concat('%', :query, '%')) " +
                        "or lower(u.email) like lower(concat('%', :query, '%'))) order by u.username")
        List<User> searchActiveUsers(@Param("query") String query,
                        @Param("currentUserId") Long currentUserId,
                        Pageable pageable);

}
