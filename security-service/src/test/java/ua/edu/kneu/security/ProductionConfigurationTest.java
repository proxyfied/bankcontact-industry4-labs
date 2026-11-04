package ua.edu.kneu.security;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
@ActiveProfiles("prod")
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:prod;DB_CLOSE_DELAY=-1", "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.properties.hibernate.default_schema=PUBLIC", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
class ProductionConfigurationTest {
 @Autowired MockMvc mvc; @Autowired Environment env;
 @Test void productionCookieAndCorrectLogoutName() throws Exception {
  assertThat(env.getProperty("server.servlet.session.cookie.name")).isEqualTo("JSESSIONID");
  assertThat(env.getProperty("server.servlet.session.cookie.secure")).isEqualTo("true");
  assertThat(env.getProperty("server.forward-headers-strategy")).isEqualTo("native");
  mvc.perform(post("/logout").with(user("production.user")).with(csrf()))
   .andExpect(status().is3xxRedirection()).andExpect(cookie().maxAge("JSESSIONID",0));
 }
 @Test void healthIsPublicWithoutInternalDetailsAndOtherActuatorIsClosed() throws Exception {
  mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP")).andExpect(jsonPath("$.components").doesNotExist());
  mvc.perform(get("/actuator/env").with(user("production.user").roles("ADMIN"))).andExpect(status().isForbidden());
 }
 @Test void productionRefusesPublicDemoCredentials() {
  MockEnvironment environment=new MockEnvironment();environment.setActiveProfiles("prod","demo");
  assertThatThrownBy(()->new ProductionProfileGuard(environment).run(new DefaultApplicationArguments()))
   .isInstanceOf(IllegalStateException.class);
 }
}
