package ua.edu.kneu.bankcontact.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import ua.edu.kneu.bankcontact.entity.Interaction;
public interface InteractionRepository extends JpaRepository<Interaction, Long> {}
