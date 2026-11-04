package ua.edu.kneu.bankcontact.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "tickets", indexes = {
    @Index(name = "idx_tickets_customer", columnList = "customer_id"),
    @Index(name = "idx_tickets_agent_status", columnList = "assigned_agent_id,status")
})
@Getter @Setter @NoArgsConstructor
public class Ticket extends BaseEntity {
    @Column(nullable = false, unique = true, length = 40)
    private String ticketCode;
    @Column(nullable = false, length = 180)
    private String subject;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.NEW;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_ticket_customer"))
    private Customer customer;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id",
        foreignKey = @ForeignKey(name = "fk_ticket_agent"))
    private Agent assignedAgent;
    @OneToMany(mappedBy = "ticket", fetch = FetchType.LAZY)
    @Setter(AccessLevel.NONE)
    private Set<Interaction> interactions = new HashSet<>();
    public void assignTo(Agent agent) {
        assignedAgent = agent;
        agent.getTickets().add(this);
    }
    public void addInteraction(Interaction interaction) {
        interactions.add(interaction);
        interaction.setTicket(this);
    }
}
