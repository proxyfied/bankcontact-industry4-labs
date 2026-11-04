package ua.edu.kneu.bankcontact.repository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import ua.edu.kneu.bankcontact.entity.Agent;

public interface AgentRepository extends JpaRepository<Agent, Long> {
    @Override
    @EntityGraph(attributePaths = "department")
    Page<Agent> findAll(Pageable pageable);
}
