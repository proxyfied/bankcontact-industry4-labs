package ua.edu.kneu.bankcontact.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    private final String[] allowedOrigins;
    public CorsConfig(@Value("${app.cors.allowed-origins}") String origins) {
        allowedOrigins = Arrays.stream(origins.split(","))
            .map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
    }
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(allowedOrigins)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("Content-Type", "Accept", "Authorization")
            .exposedHeaders("Location")
            .allowCredentials(false)
            .maxAge(3600);
    }
}
