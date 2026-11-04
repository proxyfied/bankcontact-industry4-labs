package ua.edu.kneu.bankcontact.dto;
import java.time.Instant;
import java.util.List;

public record AgentResponse(Long id, String employeeCode, String fullName,
    Long departmentId, String departmentName, List<SkillView> skills,
    Instant createdAt, Instant updatedAt) {
    public record SkillView(Long id, String code, String name) {}
}
