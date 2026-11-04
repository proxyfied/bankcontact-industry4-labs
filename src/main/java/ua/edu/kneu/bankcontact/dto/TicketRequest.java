package ua.edu.kneu.bankcontact.dto;
import jakarta.validation.constraints.*;
import ua.edu.kneu.bankcontact.entity.TicketStatus;

public record TicketRequest(
    @NotBlank @Size(max = 40) String ticketCode,
    @NotBlank @Size(max = 180) String subject,
    @NotBlank @Size(max = 10000) String description,
    @NotNull TicketStatus status,
    @NotNull @Positive Long customerId,
    @Positive Long assignedAgentId
) {}
