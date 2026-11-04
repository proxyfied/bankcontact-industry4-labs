package ua.edu.kneu.articles;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
@Component
public class ArticlePromptFactory {
    private final String rules;
    public ArticlePromptFactory() throws IOException {
        rules = new ClassPathResource("prompts/article-system.txt").getContentAsString(StandardCharsets.UTF_8);
    }
    public Prompt create(String message) {
        var template = new PromptTemplate("{message}");
        String userText = template.render(Map.of("message", message));
        return new Prompt(List.of(new SystemMessage(rules), new UserMessage(userText)));
    }
}
