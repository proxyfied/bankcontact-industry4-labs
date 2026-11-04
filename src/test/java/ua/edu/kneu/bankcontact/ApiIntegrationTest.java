package ua.edu.kneu.bankcontact;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import ua.edu.kneu.bankcontact.repository.*;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired DepartmentRepository departments;
    @Autowired CustomerRepository customers;
    @Autowired AgentRepository agents;
    @Autowired TicketRepository tickets;
    @Autowired SkillRepository skills;
    private String code() { return UUID.randomUUID().toString().substring(0, 12); }
    private long create(String route, String body) throws Exception {
        String json = mvc.perform(post("/api/" + route)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(header().exists("Location"))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.parse(json).read("$.id", Long.class);
    }
    @Test void customerCrudAndPagination() throws Exception {
        String code = code();
        long id = create("customers", """
            {"externalCode":"%s","displayName":"Test customer"}
            """.formatted(code));
        mvc.perform(get("/api/customers/{id}", id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.externalCode").value(code));
        mvc.perform(put("/api/customers/{id}", id).contentType(MediaType.APPLICATION_JSON)
            .content(""" 
                {"externalCode":"%s","displayName":"Updated customer"}
                """.formatted(code)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("Updated customer"));
        mvc.perform(get("/api/customers").param("size", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.size").value(1));
        mvc.perform(delete("/api/customers/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/customers/{id}", id)).andExpect(status().isNotFound());
    }
    @Test void agentCrudUpdatesManyToMany() throws Exception {
        long department = departments.findAll().get(0).getId();
        long skill = skills.findAll().get(0).getId();
        String code = code();
        long id = create("agents", """
            {"employeeCode":"%s","fullName":"Test agent","departmentId":%d,"skillIds":[%d]}
            """.formatted(code, department, skill));
        mvc.perform(get("/api/agents/{id}", id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.skills.length()").value(1));
        mvc.perform(put("/api/agents/{id}", id).contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"employeeCode":"%s","fullName":"Updated agent","departmentId":%d,"skillIds":[]}
                """.formatted(code, department)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.skills.length()").value(0));
        mvc.perform(delete("/api/agents/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/agents/{id}", id)).andExpect(status().isNotFound());
    }
    @Test void ticketCrudCanUnassignAgent() throws Exception {
        long customer = customers.findAll().get(0).getId();
        long agent = agents.findAll().get(0).getId();
        String code = code();
        long id = create("tickets", """
            {"ticketCode":"%s","subject":"Question","description":"Test data",
             "status":"NEW","customerId":%d,"assignedAgentId":%d}
            """.formatted(code, customer, agent));
        mvc.perform(get("/api/tickets/{id}", id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("NEW"));
        mvc.perform(put("/api/tickets/{id}", id).contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"ticketCode":"%s","subject":"Updated question","description":"Updated",
                 "status":"RESOLVED","customerId":%d,"assignedAgentId":null}
                """.formatted(code, customer)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVED"))
            .andExpect(jsonPath("$.assignedAgentId").isEmpty());
        mvc.perform(delete("/api/tickets/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/tickets/{id}", id)).andExpect(status().isNotFound());
    }
    @Test void invalidFieldsReturnJson400() throws Exception {
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"externalCode":"","displayName":""}
                """))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.errors.externalCode").exists());
    }
    @Test void unknownReferenceReturns404() throws Exception {
        mvc.perform(post("/api/agents").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"employeeCode":"UNKNOWN-REF","fullName":"Example",
                 "departmentId":999999,"skillIds":[]}
                """)).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }
    @Test void unknownRecordReturns404() throws Exception {
        mvc.perform(get("/api/tickets/999999")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }
    @Test void duplicateBusinessCodeReturns409() throws Exception {
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"externalCode":"C-001","displayName":"Duplicate"}
                """))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
    }
    @Test void customerHistoryCannotBeDeleted() throws Exception {
        long id = customers.findAll().stream()
            .filter(c -> c.getExternalCode().equals("C-001")).findFirst().orElseThrow().getId();
        mvc.perform(delete("/api/customers/{id}", id)).andExpect(status().isConflict());
    }
    @Test void assignedAgentCannotBeDeleted() throws Exception {
        long id = agents.findAll().stream()
            .filter(a -> a.getEmployeeCode().equals("OP-001")).findFirst().orElseThrow().getId();
        mvc.perform(delete("/api/agents/{id}", id)).andExpect(status().isConflict());
    }
    @Test void interactionHistoryCannotBeDeleted() throws Exception {
        long id = tickets.findAll().stream()
            .filter(t -> t.getTicketCode().equals("T-001")).findFirst().orElseThrow().getId();
        mvc.perform(delete("/api/tickets/{id}", id)).andExpect(status().isConflict());
    }
    @Test void allowedCorsPreflight() throws Exception {
        mvc.perform(options("/api/tickets")
            .header("Origin", "http://localhost:5173")
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "content-type"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
            .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }
    @Test void otherCorsOriginIsRejected() throws Exception {
        mvc.perform(options("/api/tickets").header("Origin", "https://other.example")
            .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
    @Test void excessivePageSizeReturns400() throws Exception {
        mvc.perform(get("/api/tickets").param("size", "101"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }
    @Test void invalidIdentifierReturns400() throws Exception {
        mvc.perform(get("/api/customers/not-a-number")).andExpect(status().isBadRequest());
    }
    @Test void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }
    @Test void nonJsonBodyReturns415() throws Exception {
        mvc.perform(post("/api/customers").contentType(MediaType.TEXT_PLAIN).content("text"))
            .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.status").value(415));
    }
}
