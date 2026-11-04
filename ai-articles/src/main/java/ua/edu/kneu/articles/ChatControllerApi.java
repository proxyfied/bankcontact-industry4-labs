package ua.edu.kneu.articles;
import jakarta.validation.Valid;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
@RequiredArgsConstructor
public class ChatControllerApi {
    private final OpenAiChatModel chatModel;
    private final ArticlePromptFactory prompts;
    private final ArticleResponseParser parser;
    private final AiProperties settings;
    @PostMapping(value = "/generate-all", consumes = "application/json", produces = "application/json")
    public GenerationResponse generate(@Valid @RequestBody GenerationRequest request) {
        if (!settings.configured()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
            "Не налаштовано OPENAI_API_KEY на сервері.");
        long started = System.nanoTime();
        String raw;
        try {
            var response = chatModel.call(prompts.create(request.message()));
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null)
                throw new ArticleResponseException();
            raw = response.getResult().getOutput().getText();
        } catch (ArticleResponseException ex) { throw ex; }
        catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Не вдалося отримати відповідь AI. Перевірте ключ, доступ до моделі та ліміт API.");
        }
        SaveArticleToEntity article = parser.parse(raw);
        long clock = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        return new GenerationResponse(article, clock, settings.model(), settings.testMode());
    }
}
