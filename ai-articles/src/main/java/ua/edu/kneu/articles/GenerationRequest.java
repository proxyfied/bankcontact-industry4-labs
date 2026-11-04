package ua.edu.kneu.articles;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record GenerationRequest(@NotBlank @Size(max = 12000) String message) {}
