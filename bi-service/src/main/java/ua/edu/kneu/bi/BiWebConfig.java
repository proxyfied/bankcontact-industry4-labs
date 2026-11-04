package ua.edu.kneu.bi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
@Configuration
public class BiWebConfig implements WebMvcConfigurer {
 @Value("${bi.cors-origins}") private String[] origins;
 @Override public void addCorsMappings(CorsRegistry registry) {
  registry.addMapping("/api/**").allowedOrigins(origins).allowedMethods("GET","POST","OPTIONS").allowedHeaders("Content-Type","Accept");
 }
}
