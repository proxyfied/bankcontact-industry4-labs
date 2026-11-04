package ua.edu.kneu.security;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;
@Configuration
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
 @Bean SessionRegistry sessionRegistry() { return new SessionRegistryImpl(); }
 @Bean HttpSessionEventPublisher httpSessionEventPublisher() { return new HttpSessionEventPublisher(); }
 @Bean SecurityFilterChain filterChain(HttpSecurity http,SessionRegistry sessions) throws Exception {
  http.authorizeHttpRequests(auth->auth
   .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
   .requestMatchers("/","/security.css","/favicon.ico","/access-denied").permitAll()
   .requestMatchers("/login","/registration").anonymous()
   .requestMatchers("/user","/user/**").hasAnyRole("USER","ADMIN","MANAGER")
   .requestMatchers("/manager","/manager/**").hasRole("MANAGER")
   .requestMatchers("/admin","/admin/**").hasRole("ADMIN")
   .anyRequest().denyAll())
   .formLogin(form->form.loginPage("/login").loginProcessingUrl("/login")
    .defaultSuccessUrl("/user",true).failureUrl("/login?error"))
   .logout(logout->logout.logoutUrl("/logout").logoutSuccessUrl("/")
    .invalidateHttpSession(true).deleteCookies("BANKCONTACT_AUTH"))
   .exceptionHandling(errors->errors.accessDeniedPage("/access-denied"))
   .sessionManagement(session->session.sessionFixation(fixation->fixation.changeSessionId())
    .maximumSessions(1).maxSessionsPreventsLogin(false).expiredUrl("/login?expired").sessionRegistry(sessions));
  return http.build();
 }
}
