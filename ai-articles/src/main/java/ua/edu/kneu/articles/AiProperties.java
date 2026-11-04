package ua.edu.kneu.articles;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class AiProperties {
    @Value("${spring.ai.openai.api-key}") private String key;
    @Value("${spring.ai.openai.chat.options.model}") private String model;
    @Value("${app.ai.test-mode:false}") private boolean testMode;
    public boolean configured() { return key != null && !key.isBlank() && !key.equals("not-configured"); }
    public String model() { return model; }
    public boolean testMode() { return testMode; }
}
