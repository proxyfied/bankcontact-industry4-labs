package ua.edu.kneu.bankcontact.dto;
import jakarta.validation.constraints.*;

public record CustomerRequest(
    @NotBlank @Size(max = 32) String externalCode,
    @NotBlank @Size(max = 120) String displayName
) {}
