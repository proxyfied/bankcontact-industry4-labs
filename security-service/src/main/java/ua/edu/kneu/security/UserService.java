package ua.edu.kneu.security;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class UserService implements UserDetailsService {
 private final UserRepository users; private final RoleRepository roles; private final PasswordEncoder encoder;
 public UserService(UserRepository users,RoleRepository roles,PasswordEncoder encoder) { this.users=users;this.roles=roles;this.encoder=encoder; }
 public static String canonical(String username) { return username==null ? "" : username.toLowerCase(Locale.ROOT); }
 @Override @Transactional(readOnly=true)
 public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
  User user=users.findByUsername(canonical(username)).orElseThrow(()->new UsernameNotFoundException("Unknown user"));
  return new org.springframework.security.core.userdetails.User(user.getUsername(),user.getPassword(),
   user.getRoles().stream().map(role->new SimpleGrantedAuthority(role.getName())).toList());
 }
 @Transactional
 public void register(RegistrationForm form) {
  String name=canonical(form.getUsername());
  if(!name.matches("[a-z0-9._-]{3,40}")) throw new IllegalArgumentException("Неприпустимий логін.");
  String password=form.getPassword();
  if(password==null || password.length()<8 || password.getBytes(StandardCharsets.UTF_8).length>72)
   throw new IllegalArgumentException("Пароль: від 8 символів, не більше 72 байтів UTF-8.");
  if(!password.equals(form.getConfirmation())) throw new IllegalArgumentException("Паролі не збігаються.");
  if(users.existsByUsername(name)) throw new IllegalArgumentException("Цей логін уже зайнятий.");
  save(name,password,"ROLE_USER");
 }
 @Transactional
 public void createDemo(String name,String password,String role) {
  if(!users.existsByUsername(name)) save(name,password,role);
 }
 private void save(String name,String password,String roleName) {
  User user=new User();user.setUsername(name);user.setPassword(encoder.encode(password));
  user.getRoles().add(roles.findByName(roleName).orElseThrow());
  users.saveAndFlush(user);
 }
}
