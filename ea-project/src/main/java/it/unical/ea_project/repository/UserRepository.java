package it.unical.ea_project.repository;

import it.unical.ea_project.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

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