package ua.edu.kneu.bankcontact.service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.kneu.bankcontact.dto.*;
import ua.edu.kneu.bankcontact.entity.*;
import ua.edu.kneu.bankcontact.exception.*;
import ua.edu.kneu.bankcontact.repository.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgentService {
    private final AgentRepository agents;
    private final DepartmentRepository departments;
    private final SkillRepository skills;
    private final TicketRepository tickets;
    public PageResponse<AgentResponse> list(int page, int size) {
        return PageResponse.from(agents.findAll(
            PageRequest.of(page, size, Sort.by("id"))).map(this::response));
    }
    public AgentResponse get(Long id) { return response(find(id)); }
    @Transactional
    public AgentResponse create(AgentRequest request) {
        Agent item = new Agent();
        apply(item, request);
        return response(agents.saveAndFlush(item));
    }
    @Transactional
    public AgentResponse update(Long id, AgentRequest request) {
        Agent item = find(id);
        apply(item, request);
        return response(agents.saveAndFlush(item));
    }
    @Transactional
    public void delete(Long id) {
        Agent item = find(id);
        if (tickets.existsByAssignedAgentId(id))
            throw new ResourceConflictException("Agent has assigned tickets; reassign them first");
        for (Skill skill : new HashSet<>(item.getSkills())) skill.getAgents().remove(item);
        item.getSkills().clear();
        item.getDepartment().getAgents().remove(item);
        agents.delete(item);
        agents.flush();
    }
    private Agent find(Long id) {
        return agents.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agent", id));
    }
    private void apply(Agent item, AgentRequest request) {
        Department department = departments.findById(request.departmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Department", request.departmentId()));
        List<Skill> selected = request.skillIds().stream().sorted().map(id ->
            skills.findById(id).orElseThrow(() -> new ResourceNotFoundException("Skill", id))).toList();
        item.setEmployeeCode(request.employeeCode());
        item.setFullName(request.fullName());
        if (item.getDepartment() != null) item.getDepartment().getAgents().remove(item);
        department.addAgent(item);
        for (Skill skill : new HashSet<>(item.getSkills())) skill.getAgents().remove(item);
        item.getSkills().clear();
        selected.forEach(item::addSkill);
    }
    private AgentResponse response(Agent item) {
        List<AgentResponse.SkillView> views = item.getSkills().stream()
            .sorted(Comparator.comparing(Skill::getId))
            .map(s -> new AgentResponse.SkillView(s.getId(), s.getCode(), s.getName())).toList();
        return new AgentResponse(item.getId(), item.getEmployeeCode(), item.getFullName(),
            item.getDepartment().getId(), item.getDepartment().getName(), views,
            item.getCreatedAt(), item.getUpdatedAt());
    }
}
