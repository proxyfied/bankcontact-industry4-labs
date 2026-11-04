package ua.edu.kneu.security;
import org.springframework.boot.*;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
@Component @Profile("demo") @Order(20)
public class DemoAccounts implements ApplicationRunner {
 private final UserService users;private final Environment env;
 public DemoAccounts(UserService users,Environment env) { this.users=users;this.env=env; }
 @Override public void run(ApplicationArguments args) {
  users.createDemo("demo.user",env.getProperty("DEMO_USER_PASSWORD","UserDemo!2026"),"ROLE_USER");
  users.createDemo("demo.manager",env.getProperty("DEMO_MANAGER_PASSWORD","ManagerDemo!2026"),"ROLE_MANAGER");
  users.createDemo("demo.admin",env.getProperty("DEMO_ADMIN_PASSWORD","AdminDemo!2026"),"ROLE_ADMIN");
 }
}
