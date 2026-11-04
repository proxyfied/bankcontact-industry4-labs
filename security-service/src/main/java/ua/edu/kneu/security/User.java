package ua.edu.kneu.security;
import jakarta.persistence.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="sec_users") @Getter @Setter
public class User {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=40) private String username;
 @Column(nullable=false,length=100) private String password;
 @ManyToMany(fetch=FetchType.LAZY)
 @JoinTable(name="sec_user_roles",joinColumns=@JoinColumn(name="user_id"),inverseJoinColumns=@JoinColumn(name="role_id"))
 private Set<Role> roles=new HashSet<>();
}
