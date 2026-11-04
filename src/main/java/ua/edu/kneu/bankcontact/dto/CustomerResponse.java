package ua.edu.kneu.bankcontact.dto;
import java.time.Instant;

public record CustomerResponse(Long id, String externalCode,
    String displayName, Instant createdAt, Instant updatedAt) {}
