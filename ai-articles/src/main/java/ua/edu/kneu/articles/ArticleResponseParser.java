package ua.edu.kneu.articles;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Set;
import org.springframework.stereotype.Component;
@Component
public class ArticleResponseParser {
    private final ObjectMapper mapper;
    public ArticleResponseParser(ObjectMapper objectMapper) {
        this.mapper = objectMapper.copy()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    }
    public SaveArticleToEntity parse(String raw) {
        if (raw == null || raw.length() > 65536) throw new ArticleResponseException();
        String json = raw.strip();
        if (json.startsWith("```")) {
            if (!json.matches("(?s)^```(?:json)?\\s*\\{.*}\\s*```$")) throw new ArticleResponseException();
            json = json.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        try {
            var tree = mapper.readTree(json);
            if (tree == null || !tree.isObject()) throw new ArticleResponseException();
            SaveArticleToEntity article = mapper.readValue(json, SaveArticleToEntity.class);
            validate(article);
            return article;
        } catch (IOException | IllegalArgumentException ex) {
            throw new ArticleResponseException();
        }
    }
    private void validate(SaveArticleToEntity a) {
        if (!Set.of("", "ukr", "eng", "other").contains(a.language())) throw new ArticleResponseException();
        for (String flag : new String[]{a.published_in_ukraine(), a.published_abroad(),
                a.indexed_scopus(), a.indexed_web_of_science(), a.peer_reviewed(), a.open_access()}) {
            if (!Set.of("0", "1").contains(flag)) throw new ArticleResponseException();
        }
        if (a.published_in_ukraine().equals("1") && a.published_abroad().equals("1")) throw new ArticleResponseException();
        int first = positiveOrUnknown(a.first_page());
        int last = positiveOrUnknown(a.last_page());
        int pages = positiveOrUnknown(a.total_volume_of_printed_sheets());
        if (first > 0 && last > 0 && (last < first || pages != last - first + 1)) throw new ArticleResponseException();
    }
    private int positiveOrUnknown(String value) {
        try {
            if (!value.matches("[0-9]{1,7}")) throw new NumberFormatException();
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) { throw new ArticleResponseException(); }
    }
}
