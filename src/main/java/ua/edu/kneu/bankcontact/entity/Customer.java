package ua.edu.kneu.bankcontact.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "customers")
@Getter @Setter @NoArgsConstructor
public class Customer extends BaseEntity {
    @Column(nullable = false, unique = true, length = 32)
    private String externalCode;
    @Column(nullable = false, length = 120)
    private String displayName;
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    @Setter(AccessLevel.NONE)
    private Set<Ticket> tickets = new HashSet<>();
    public void addTicket(Ticket ticket) {
        tickets.add(ticket);
        ticket.setCustomer(this);
    }
}
