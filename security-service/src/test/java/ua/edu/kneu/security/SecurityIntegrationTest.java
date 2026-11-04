package ua.edu.kneu.security;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
@SpringBootTest(properties={
 "spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:security;DB_CLOSE_DELAY=-1}",
 "spring.datasource.username=${TEST_DB_USER:sa}","spring.datasource.password=${TEST_DB_PASSWORD:}",
 "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
class SecurityIntegrationTest {
 @Autowired MockMvc mvc; @Autowired UserService service; @Autowired UserRepository users;
 @Autowired PasswordEncoder encoder; @Autowired SessionRegistry sessions;
 @BeforeEach void clear() {
  users.deleteAll();
  for(Object p:sessions.getAllPrincipals()) for(var s:sessions.getAllSessions(p,true)) sessions.removeSessionInformation(s.getSessionId());
 }
 private void register(String username,String password) {
  RegistrationForm f=new RegistrationForm();f.setUsername(username);f.setPassword(password);f.setConfirmation(password);service.register(f);
 }
 @Test void registrationStoresOnlyBcryptAndDefaultUserRole() throws Exception {
  mvc.perform(post("/registration").with(csrf()).param("username","New.User").param("password","SecurePass!2026").param("confirmation","SecurePass!2026").param("roles","ROLE_ADMIN"))
   .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?registered"));
  User u=users.findByUsername("new.user").orElseThrow();
  assertThat(u.getPassword()).startsWith("$2a$12$");assertThat(encoder.matches("SecurePass!2026",u.getPassword())).isTrue();
  assertThat(u.getRoles()).extracting(Role::getName).containsExactly("ROLE_USER");
 }
 @Test void rejectsDuplicatesCaseInsensitivelyWithoutEchoingPassword() throws Exception {
  register("same.user","SecurePass!2026");
  mvc.perform(post("/registration").with(csrf()).param("username","SAME.USER").param("password","OtherPass!2026").param("confirmation","OtherPass!2026"))
   .andExpect(status().isBadRequest()).andExpect(content().string(org.hamcrest.Matchers.containsString("Цей логін уже зайнятий")))
   .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("OtherPass!2026"))));
  assertThat(users.count()).isEqualTo(1);
 }
 @Test void rejectsMismatchInvalidNameAndTooManyUtf8Bytes() throws Exception {
  for(String[] data:List.of(new String[]{"good.user","abcdefgh","different"},new String[]{"bad name","abcdefgh","abcdefgh"},new String[]{"good.user","я".repeat(37),"я".repeat(37)})) {
   mvc.perform(post("/registration").with(csrf()).param("username",data[0]).param("password",data[1]).param("confirmation",data[2])).andExpect(status().isBadRequest());
  }
  assertThat(users.count()).isZero();
 }
 @Test void loginUsesDatabaseAndRejectsIncorrectPassword() throws Exception {
  register("db.user","SecurePass!2026");
  mvc.perform(post("/login").with(csrf()).param("username","DB.USER").param("password","wrong")).andExpect(redirectedUrl("/login?error"));
  var res=mvc.perform(post("/login").with(csrf()).param("username","DB.USER").param("password","SecurePass!2026")).andExpect(redirectedUrl("/user")).andReturn();
  mvc.perform(get("/user").session((MockHttpSession)res.getRequest().getSession(false))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("db.user")));
 }
 @ParameterizedTest
 @CsvSource({"USER,/user,200","USER,/manager,403","USER,/admin,403","MANAGER,/user,200","MANAGER,/manager,200","MANAGER,/admin,403","ADMIN,/user,200","ADMIN,/manager,403","ADMIN,/admin,200"})
 void roleMatrix(String role,String path,int status) throws Exception {
  mvc.perform(get(path).with(user("matrix.user").roles(role))).andExpect(status().is(status));
 }
 @Test void anonymousPagesAndProtectedRedirect() throws Exception {
  for(String path:List.of("/","/login","/registration","/security.css")) mvc.perform(get(path)).andExpect(status().isOk());
  for(String path:List.of("/user","/admin","/manager")) mvc.perform(get(path)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
 }
 @Test void authenticatedCannotOpenLoginOrRegistration() throws Exception {
  mvc.perform(get("/login").with(user("signed.in").roles("USER"))).andExpect(status().isForbidden());
  mvc.perform(get("/registration").with(user("signed.in").roles("USER"))).andExpect(status().isForbidden());
 }
 @Test void csrfRequiredForLoginRegistrationAndLogout() throws Exception {
  mvc.perform(post("/registration").param("username","csrf.user").param("password","SecurePass!2026").param("confirmation","SecurePass!2026")).andExpect(status().isForbidden());
  mvc.perform(post("/login").param("username","csrf.user").param("password","SecurePass!2026")).andExpect(status().isForbidden());
  mvc.perform(post("/logout").with(user("signed.in"))).andExpect(status().isForbidden());
  assertThat(users.count()).isZero();
 }
 @Test void secondRealFormLoginExpiresFirstSession() throws Exception {
  register("session.user","SecurePass!2026");
  var first=mvc.perform(post("/login").with(csrf()).param("username","session.user").param("password","SecurePass!2026")).andExpect(redirectedUrl("/user")).andReturn();
  MockHttpSession a=(MockHttpSession)first.getRequest().getSession(false);
  mvc.perform(get("/user").session(a)).andExpect(status().isOk());
  var second=mvc.perform(post("/login").with(csrf()).param("username","SESSION.USER").param("password","SecurePass!2026")).andExpect(redirectedUrl("/user")).andReturn();
  MockHttpSession b=(MockHttpSession)second.getRequest().getSession(false);
  assertThat(sessions.getSessionInformation(a.getId()).isExpired()).isTrue();
  mvc.perform(get("/user").session(a)).andExpect(redirectedUrl("/login?expired"));
  mvc.perform(get("/user").session(b)).andExpect(status().isOk());
  assertThat(a.isInvalid()).isTrue();
  assertThat(sessions.getAllSessions(service.loadUserByUsername("session.user"),false)).hasSize(1);
 }
 @Test void logoutInvalidatesSessionAndReturnsHome() throws Exception {
  register("logout.user","SecurePass!2026");
  var res=mvc.perform(post("/login").with(csrf()).param("username","logout.user").param("password","SecurePass!2026")).andReturn();
  MockHttpSession session=(MockHttpSession)res.getRequest().getSession(false);
  mvc.perform(post("/logout").session(session).with(csrf())).andExpect(redirectedUrl("/"));
  assertThat(session.isInvalid()).isTrue();
 }
 @Test void hashesAreSaltedAndPasswordsNotReturnedByUserDetailsName() {
  register("first.user","SecurePass!2026"); register("second.user","SecurePass!2026");
  assertThat(users.findByUsername("first.user").orElseThrow().getPassword()).isNotEqualTo(users.findByUsername("second.user").orElseThrow().getPassword());
  assertThat(service.loadUserByUsername("FIRST.USER").getUsername()).isEqualTo("first.user");
 }
}
