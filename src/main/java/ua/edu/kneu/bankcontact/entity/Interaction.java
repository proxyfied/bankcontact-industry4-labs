package ua.edu.kneu.bankcontact.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "interactions", indexes = {
    @Index(name = "idx_interactions_ticket_time", columnList = "ticket_id,occurred_at")
})
@Getter @Setter @NoArgsConstructor
public class Interaction extends BaseEntity {
    @Column(nullable = false, unique = true, length = 64)
    private String eventCode;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Channel channel;
    @Column(nullable = false)
    private java.time.Instant occurredAt;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_interaction_ticket"))
    private Ticket ticket;
}
