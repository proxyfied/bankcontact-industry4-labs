package ua.edu.kneu.bankcontact.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "skills")
@Getter @Setter @NoArgsConstructor
public class Skill extends BaseEntity {
    @Column(nullable = false, unique = true, length = 32)
    private String code;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    @ManyToMany(mappedBy = "skills", fetch = FetchType.LAZY)
    @Setter(AccessLevel.NONE)
    private Set<Agent> agents = new HashSet<>();
}
