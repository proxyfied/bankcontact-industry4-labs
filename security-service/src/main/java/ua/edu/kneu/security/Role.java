package ua.edu.kneu.security;
import jakarta.persistence.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="sec_roles") @Getter @Setter
public class Role {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=30) private String name;
 @ManyToMany(mappedBy="roles") private Set<User> users=new HashSet<>();
}
