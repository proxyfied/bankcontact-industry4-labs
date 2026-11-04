package ua.edu.kneu.bankcontact.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import ua.edu.kneu.bankcontact.entity.Department;
public interface DepartmentRepository extends JpaRepository<Department, Long> {}
