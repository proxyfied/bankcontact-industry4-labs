package ua.edu.kneu.articles;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"spring.ai.openai.api-key=not-configured","app.ai.test-mode=false"})
@AutoConfigureMockMvc
class MissingKeyTest {
    @Autowired MockMvc mvc;
    @Test void missingKeyReturns503WithoutProviderCall() throws Exception {
        mvc.perform(post("/generate-all").contentType("application/json").content("{\"message\":\"Тест\"}" )).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.detail").value("Не налаштовано OPENAI_API_KEY на сервері."));
    }
    @Test void pageLoadsWithoutKey() throws Exception {
        mvc.perform(get("/articles")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("AI-сервіс ще не підключено")));
    }
}
