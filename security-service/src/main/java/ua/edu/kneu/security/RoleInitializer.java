package ua.edu.kneu.security;
import java.util.List;
import org.springframework.boot.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component @Order(10)
public class RoleInitializer implements ApplicationRunner {
 private final RoleRepository roles;
 public RoleInitializer(RoleRepository roles) { this.roles=roles; }
 @Override @Transactional public void run(ApplicationArguments args) {
  for(String name:List.of("ROLE_USER","ROLE_MANAGER","ROLE_ADMIN")) {
   if(roles.findByName(name).isEmpty()) {Role role=new Role();role.setName(name);roles.save(role);}
  }
 }
}
