package ua.edu.kneu.security;
import java.util.Arrays;
import org.springframework.boot.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
@Component @Order(0)
public class ProductionProfileGuard implements ApplicationRunner {
 private final Environment environment;
 public ProductionProfileGuard(Environment environment) { this.environment=environment; }
 @Override public void run(ApplicationArguments args) {
  var profiles=Arrays.asList(environment.getActiveProfiles());
  if(profiles.contains("prod") && profiles.contains("demo"))
   throw new IllegalStateException("The prod and demo profiles cannot be enabled together.");
 }
}
