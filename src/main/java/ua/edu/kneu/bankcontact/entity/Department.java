package ua.edu.kneu.bankcontact.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "departments")
@Getter @Setter @NoArgsConstructor
public class Department extends BaseEntity {
    @Column(nullable = false, unique = true, length = 32)
    private String code;
    @Column(nullable = false, length = 120)
    private String name;
    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    @Setter(AccessLevel.NONE)
    private Set<Agent> agents = new HashSet<>();
    public void addAgent(Agent agent) {
        agents.add(agent);
        agent.setDepartment(this);
    }
}
