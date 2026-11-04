package ua.edu.kneu.bankcontact.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import ua.edu.kneu.bankcontact.entity.Agent;
public interface AgentRepository extends JpaRepository<Agent, Long> {}
