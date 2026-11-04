package ua.edu.kneu.articles;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc
class ArticleHttpIntegrationTest {
    static HttpServer upstream;
    static final AtomicReference<String> content = new AtomicReference<>();
    static final AtomicReference<String> sent = new AtomicReference<>();
    static final AtomicInteger status = new AtomicInteger(200), calls = new AtomicInteger();
    static {
        try {
            upstream = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            upstream.createContext("/v1/chat/completions", exchange -> {
                calls.incrementAndGet(); sent.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                var mapper = new ObjectMapper();
                byte[] bytes = mapper.writeValueAsBytes(status.get()==200 ? Map.of(
                    "id","stub-test", "object","chat.completion", "created",1, "model","gpt-4o-mini",
                    "choices",List.of(Map.of("index",0,"message",Map.of("role","assistant","content",content.get()),"finish_reason","stop")))
                    : Map.of("error",Map.of("message","test-provider-rejected", "type","invalid_request_error")));
                exchange.getResponseHeaders().add("Content-Type","application/json");
                exchange.sendResponseHeaders(status.get(),bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
            });upstream.start();
        } catch(Exception ex) { throw new ExceptionInInitializerError(ex); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.base-url", () -> "http://127.0.0.1:"+upstream.getAddress().getPort());
        registry.add("spring.ai.openai.api-key", () -> "test-key-not-real");
        registry.add("app.ai.test-mode", () -> true);
    }
    @Autowired MockMvc mvc; @Autowired ObjectMapper mapper;
    @BeforeEach void reset() throws Exception {
        status.set(200); content.set(new ClassPathResource("fixtures/article.json").getContentAsString(StandardCharsets.UTF_8));
    }
    @AfterAll static void stop() { upstream.stop(0); }
    @Test void realSpringAiCallsHttpAndJacksonMapsRecord() throws Exception {
        mvc.perform(post("/generate-all").contentType("application/json").content("{\"message\":\"Тестове цитування\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.article.total_volume_of_printed_sheets").value("11"))
            .andExpect(jsonPath("$.article.language").value("ukr")).andExpect(jsonPath("$.clock").isNumber())
            .andExpect(jsonPath("$.testMode").value(true));
        var body = mapper.readTree(sent.get());
        assertThat(body.path("model").asText()).isEqualTo("gpt-4o-mini");
        assertThat(body.path("messages").get(0).path("role").asText()).isEqualTo("system");
        assertThat(body.path("messages").get(1).path("content").asText()).contains("Тестове цитування");
        assertThat(body.path("response_format").path("type").asText()).isEqualTo("json_object");
    }
    @Test void missingInputReturns400WithoutCallingProvider() throws Exception {
        int before = calls.get();
        mvc.perform(post("/generate-all").contentType("application/json").content("{\"message\":\"   \"}" )).andExpect(status().isBadRequest());
        assertThat(calls.get()).isEqualTo(before);
    }
    @Test void oversizedInputReturns400() throws Exception {
        mvc.perform(post("/generate-all").contentType("application/json").content(mapper.writeValueAsString(Map.of("message","a".repeat(12001))))).andExpect(status().isBadRequest());
    }
    @Test void malformedRequestReturns400() throws Exception {
        mvc.perform(post("/generate-all").contentType("application/json").content("{" )).andExpect(status().isBadRequest());
    }
    @Test void wrongContentTypeReturns415() throws Exception {
        mvc.perform(post("/generate-all").contentType("text/plain").content("text" )).andExpect(status().isUnsupportedMediaType());
    }
    @Test void invalidLlmJsonReturns502() throws Exception {
        content.set("not JSON");
        mvc.perform(post("/generate-all").contentType("application/json").content("{\"message\":\"тест\"}" )).andExpect(status().isBadGateway()).andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("JSON")));
    }
    @Test void upstreamFailureIsSanitized() throws Exception {
        status.set(401);
        var response = mvc.perform(post("/generate-all").contentType("application/json").content("{\"message\":\"тест\"}" )).andExpect(status().isBadGateway()).andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("test-provider-rejected","test-key-not-real");
    }
    @Test void thymeleafPageClearlyLabelsStubMode() throws Exception {
        mvc.perform(get("/articles")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("ТЕСТОВИЙ РЕЖИМ")));
    }
}
