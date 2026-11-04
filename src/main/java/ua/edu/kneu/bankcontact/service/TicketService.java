package ua.edu.kneu.bankcontact.service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.kneu.bankcontact.dto.*;
import ua.edu.kneu.bankcontact.entity.*;
import ua.edu.kneu.bankcontact.exception.*;
import ua.edu.kneu.bankcontact.repository.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketService {
    private final TicketRepository tickets;
    private final CustomerRepository customers;
    private final AgentRepository agents;
    private final InteractionRepository interactions;
    public PageResponse<TicketResponse> list(int page, int size) {
        return PageResponse.from(tickets.findAll(
            PageRequest.of(page, size, Sort.by("id"))).map(this::response));
    }
    public TicketResponse get(Long id) { return response(find(id)); }
    @Transactional
    public TicketResponse create(TicketRequest request) {
        Ticket item = new Ticket();
        apply(item, request);
        return response(tickets.saveAndFlush(item));
    }
    @Transactional
    public TicketResponse update(Long id, TicketRequest request) {
        Ticket item = find(id);
        apply(item, request);
        return response(tickets.saveAndFlush(item));
    }
    @Transactional
    public void delete(Long id) {
        Ticket item = find(id);
        if (interactions.existsByTicketId(id))
            throw new ResourceConflictException("Ticket has interactions; preserve the service history");
        item.getCustomer().getTickets().remove(item);
        if (item.getAssignedAgent() != null) item.getAssignedAgent().getTickets().remove(item);
        tickets.delete(item);
        tickets.flush();
    }
    private Ticket find(Long id) {
        return tickets.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
    }
    private void apply(Ticket item, TicketRequest request) {
        Customer customer = customers.findById(request.customerId())
            .orElseThrow(() -> new ResourceNotFoundException("Customer", request.customerId()));
        Agent agent = request.assignedAgentId() == null ? null :
            agents.findById(request.assignedAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent", request.assignedAgentId()));
        item.setTicketCode(request.ticketCode());
        item.setSubject(request.subject());
        item.setDescription(request.description());
        item.setStatus(request.status());
        if (item.getCustomer() != null) item.getCustomer().getTickets().remove(item);
        customer.addTicket(item);
        if (item.getAssignedAgent() != null) item.getAssignedAgent().getTickets().remove(item);
        item.setAssignedAgent(null);
        if (agent != null) item.assignTo(agent);
    }
    private TicketResponse response(Ticket item) {
        return new TicketResponse(item.getId(), item.getTicketCode(), item.getSubject(),
            item.getDescription(), item.getStatus(), item.getCustomer().getId(),
            item.getAssignedAgent() == null ? null : item.getAssignedAgent().getId(),
            item.getCreatedAt(), item.getUpdatedAt());
    }
}
