package ua.edu.kneu.security;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
@Controller
public class UserController {
 private final UserService users; private final SessionRegistry sessions;
 public UserController(UserService users,SessionRegistry sessions) { this.users=users;this.sessions=sessions; }
 @Value("${bankcontact.url}") private String bankcontactUrl;
 @ModelAttribute("bankcontactUrl") String bankcontactUrl() { return bankcontactUrl; }
 @InitBinder("registrationForm") void fields(WebDataBinder binder) { binder.setAllowedFields("username","password","confirmation"); }
 @GetMapping("/") String index() { return "index"; }
 @GetMapping("/login") String login() { return "login"; }
 @GetMapping("/registration") String registration(Model model) { model.addAttribute("registrationForm",new RegistrationForm()); return "registration"; }
 @PostMapping("/registration") String register(@Valid @ModelAttribute RegistrationForm registrationForm,BindingResult result,HttpServletResponse response) {
  if(!result.hasErrors()) {
   try { users.register(registrationForm); return "redirect:/login?registered"; }
   catch(IllegalArgumentException ex) { result.reject("registration",ex.getMessage()); }
   catch(DataIntegrityViolationException ex) { result.reject("registration","Цей логін уже зайнятий."); }
  }
  registrationForm.setPassword("");registrationForm.setConfirmation("");
  response.setStatus(HttpStatus.BAD_REQUEST.value());return "registration";
 }
 @GetMapping({"/user","/user/"}) String user(Authentication auth,Model model) {
  model.addAttribute("username",auth.getName()); model.addAttribute("roles",auth.getAuthorities());return "user-page";
 }
 public record ActiveUser(String username,int sessions) {}
 @GetMapping({"/admin","/admin/"}) String admin(Model model) {
  List<ActiveUser> active=sessions.getAllPrincipals().stream().filter(UserDetails.class::isInstance).map(UserDetails.class::cast)
   .map(u->new ActiveUser(u.getUsername(),sessions.getAllSessions(u,false).size())).filter(u->u.sessions()>0)
   .sorted(Comparator.comparing(ActiveUser::username)).toList();
  model.addAttribute("activeUsers",active);return "admin-page";
 }
 @GetMapping({"/manager","/manager/"}) String manager() { return "manager-page"; }
 @RequestMapping("/access-denied") @ResponseStatus(HttpStatus.FORBIDDEN) String forbidden() { return "access-denied"; }
}
