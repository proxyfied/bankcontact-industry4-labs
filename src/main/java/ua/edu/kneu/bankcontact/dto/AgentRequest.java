package ua.edu.kneu.bankcontact.dto;
import jakarta.validation.constraints.*;
import java.util.Set;

public record AgentRequest(
    @NotBlank @Size(max = 32) String employeeCode,
    @NotBlank @Size(max = 120) String fullName,
    @NotNull @Positive Long departmentId,
    @NotNull @Size(max = 100) Set<@Positive Long> skillIds
) {}
