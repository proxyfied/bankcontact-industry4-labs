package ua.edu.kneu.bankcontact.dto;
import java.time.Instant;
import ua.edu.kneu.bankcontact.entity.TicketStatus;

public record TicketResponse(Long id, String ticketCode, String subject,
    String description, TicketStatus status, Long customerId,
    Long assignedAgentId, Instant createdAt, Instant updatedAt) {}
