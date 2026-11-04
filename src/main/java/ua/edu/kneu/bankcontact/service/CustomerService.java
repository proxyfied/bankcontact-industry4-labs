package ua.edu.kneu.bankcontact.service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.kneu.bankcontact.dto.*;
import ua.edu.kneu.bankcontact.entity.Customer;
import ua.edu.kneu.bankcontact.exception.*;
import ua.edu.kneu.bankcontact.repository.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {
    private final CustomerRepository customers;
    private final TicketRepository tickets;
    public PageResponse<CustomerResponse> list(int page, int size) {
        return PageResponse.from(customers.findAll(
            PageRequest.of(page, size, Sort.by("id"))).map(this::response));
    }
    public CustomerResponse get(Long id) { return response(find(id)); }
    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        Customer item = new Customer();
        apply(item, request);
        return response(customers.saveAndFlush(item));
    }
    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer item = find(id);
        apply(item, request);
        return response(customers.saveAndFlush(item));
    }
    @Transactional
    public void delete(Long id) {
        Customer item = find(id);
        if (tickets.existsByCustomerId(id))
            throw new ResourceConflictException("Customer has tickets; preserve the service history");
        customers.delete(item);
        customers.flush();
    }
    private Customer find(Long id) {
        return customers.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }
    private void apply(Customer item, CustomerRequest request) {
        item.setExternalCode(request.externalCode());
        item.setDisplayName(request.displayName());
    }
    private CustomerResponse response(Customer item) {
        return new CustomerResponse(item.getId(), item.getExternalCode(),
            item.getDisplayName(), item.getCreatedAt(), item.getUpdatedAt());
    }
}
