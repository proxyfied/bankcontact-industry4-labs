package ua.edu.kneu.bankcontact.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import ua.edu.kneu.bankcontact.entity.Customer;
public interface CustomerRepository extends JpaRepository<Customer, Long> {}
