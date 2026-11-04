package ua.edu.kneu.security;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
public interface UserRepository extends JpaRepository<User,Long> {
 @EntityGraph(attributePaths="roles") Optional<User> findByUsername(String username);
 boolean existsByUsername(String username);
}
