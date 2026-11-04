package ua.edu.kneu.articles;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
@RequiredArgsConstructor
public class PageController {
    private final AiProperties settings;
    @Value("${app.bankcontact-url}") private String bankcontactUrl;
    @GetMapping("/") public String index() { return "redirect:/articles"; }
    @GetMapping("/articles") public String articles(Model model) {
        model.addAttribute("configured", settings.configured());
        model.addAttribute("testMode", settings.testMode());
        model.addAttribute("model", settings.model());
        model.addAttribute("bankcontactUrl", bankcontactUrl);
        return "articles";
    }
}
