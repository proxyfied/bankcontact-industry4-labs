package ua.edu.kneu.bankcontact.config;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.kneu.bankcontact.entity.*;
import ua.edu.kneu.bankcontact.repository.*;
import java.time.Instant;

@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataLoader implements CommandLineRunner {
    private final DepartmentRepository departments;
    private final AgentRepository agents;
    private final SkillRepository skills;
    private final CustomerRepository customers;
    private final TicketRepository tickets;
    private final InteractionRepository interactions;

    @Override @Transactional
    public void run(String... args) {
        if (departments.count() > 0) return;
        Department support = department("SUPPORT", "Customer support");
        Department payments = department("PAYMENTS", "Payments support");
        Skill cards = skill("CARDS", "Payment cards");
        Skill online = skill("ONLINE", "Online banking");
        Agent first = agent("OP-001", "Demo Operator One", support);
        Agent second = agent("OP-002", "Demo Operator Two", payments);
        // Four join rows demonstrate both directions of the many-to-many.
        first.addSkill(cards); first.addSkill(online);
        second.addSkill(cards); second.addSkill(online);
        Customer c1 = customer("C-001", "Demo Customer One");
        Customer c2 = customer("C-002", "Demo Customer Two");
        Ticket t1 = ticket("T-001", "Card payment question", c1, first);
        Ticket t2 = ticket("T-002", "Online banking question", c2, second);
        interaction("EV-001", t1, Channel.PHONE);
        interaction("EV-002", t2, Channel.CHAT);
        System.out.println("DEMO: 2 rows per entity table, 4 rows in agent_skills.");
    }
    private Department department(String code, String name) {
        Department d = new Department(); d.setCode(code); d.setName(name);
        return departments.save(d);
    }
    private Skill skill(String code, String name) {
        Skill s = new Skill(); s.setCode(code); s.setName(name);
        s.setDescription("Synthetic training skill");
        return skills.save(s);
    }
    private Agent agent(String code, String name, Department department) {
        Agent a = new Agent(); a.setEmployeeCode(code); a.setFullName(name);
        department.addAgent(a);
        return agents.save(a);
    }
    private Customer customer(String code, String name) {
        Customer c = new Customer(); c.setExternalCode(code); c.setDisplayName(name);
        return customers.save(c);
    }
    private Ticket ticket(String code, String subject, Customer customer, Agent agent) {
        Ticket t = new Ticket(); t.setTicketCode(code); t.setSubject(subject);
        t.setDescription("Synthetic request for ORM validation");
        customer.addTicket(t); t.assignTo(agent);
        t.setStatus(TicketStatus.IN_PROGRESS);
        return tickets.save(t);
    }
    private void interaction(String eventCode, Ticket ticket, Channel channel) {
        Interaction i = new Interaction(); i.setEventCode(eventCode); i.setChannel(channel);
        i.setOccurredAt(Instant.parse("2026-11-04T08:00:00Z"));
        i.setSummary("Synthetic interaction event");
        ticket.addInteraction(i); interactions.save(i);
    }
}
