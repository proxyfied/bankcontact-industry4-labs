package ua.edu.kneu.bankcontact.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "agents", indexes = {
    @Index(name = "idx_agents_department", columnList = "department_id")
})
@Getter @Setter @NoArgsConstructor
public class Agent extends BaseEntity {
    @Column(nullable = false, unique = true, length = 32)
    private String employeeCode;
    @Column(nullable = false, length = 120)
    private String fullName;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_agent_department"))
    private Department department;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "agent_skills",
        joinColumns = @JoinColumn(name = "agent_id",
            foreignKey = @ForeignKey(name = "fk_agent_skill_agent")),
        inverseJoinColumns = @JoinColumn(name = "skill_id",
            foreignKey = @ForeignKey(name = "fk_agent_skill_skill")),
        indexes = @Index(name = "idx_agent_skills_skill", columnList = "skill_id"))
    @Setter(AccessLevel.NONE)
    private Set<Skill> skills = new HashSet<>();
    @OneToMany(mappedBy = "assignedAgent", fetch = FetchType.LAZY)
    @Setter(AccessLevel.NONE)
    private Set<Ticket> tickets = new HashSet<>();
    public void addSkill(Skill skill) {
        skills.add(skill);
        skill.getAgents().add(this);
    }
}
