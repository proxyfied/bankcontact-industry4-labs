package ua.edu.kneu.bi;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
@Entity
@Table(name="bi_contact_records", indexes={@Index(name="ix_bi_category",columnList="category"), @Index(name="ix_bi_status",columnList="status"), @Index(name="ix_bi_date",columnList="registered_on")})
@Getter @Setter
public class ContactRecord {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="external_code",nullable=false,unique=true,length=40) private String externalCode;
 @Column(nullable=false,length=200) private String subject;
 @Column(nullable=false,length=80) private String category;
 @Column(nullable=false,length=20) private String status;
 @Column(name="registered_on",nullable=false) private LocalDate registeredOn;
 @Column(name="handling_minutes",nullable=false) private int handlingMinutes;
}
