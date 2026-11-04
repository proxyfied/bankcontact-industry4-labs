package ua.edu.kneu.bankcontact;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.kneu.bankcontact.entity.*;
import ua.edu.kneu.bankcontact.repository.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("demo")
class OrmIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired AgentRepository agents;
    @Autowired DepartmentRepository departments;
    @Autowired EntityManager em;

    @Test void seedRowsAndSchemaExist() {
        for (String table : new String[]{"departments","agents","skills",
                "customers","tickets","interactions"}) {
            assertThat(jdbc.queryForObject("select count(*) from " + table, Long.class))
                .as(table).isEqualTo(2L);
        }
        assertThat(jdbc.queryForObject("select count(*) from agent_skills", Long.class))
            .isEqualTo(4L);
        assertThat(jdbc.queryForObject("""
            select count(*) from pg_constraint
            where connamespace='public'::regnamespace and contype='f'
            """, Long.class)).isEqualTo(6L);
        assertThat(jdbc.queryForObject("""
            select count(*) from information_schema.columns
            where table_schema='public' and column_name='id' and is_identity='YES'
            """, Long.class)).isEqualTo(6L);
        assertThat(jdbc.queryForObject("""
            select count(*) from pg_indexes where schemaname='public'
            and indexname in ('idx_agents_department','idx_tickets_customer',
              'idx_tickets_agent_status','idx_interactions_ticket_time',
              'idx_agent_skills_skill')
            """, Long.class)).isEqualTo(5L);
    }
    @Test @Transactional void associationsLoadBothWays() {
        Agent a = agents.findAll().get(0);
        assertThat(a.getSkills()).hasSize(2);
        assertThat(a.getDepartment().getAgents()).contains(a);
        assertThat(a.getTickets()).hasSize(1);
        Skill skill = a.getSkills().iterator().next();
        assertThat(skill.getAgents()).hasSize(2);
        assertThat(a.getTickets().iterator().next().getInteractions()).hasSize(1);
    }
    @Test @Transactional void duplicateBusinessCodeIsRejected() {
        Department copy = new Department();
        copy.setCode("SUPPORT"); copy.setName("Duplicate");
        assertThatThrownBy(() -> departments.saveAndFlush(copy))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test @Transactional void missingParentIsRejected() {
        assertThatThrownBy(() -> jdbc.update("""
            insert into agent_skills(agent_id, skill_id)
            values (999999, 999999)
            """)).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test @Transactional void duplicateJoinPairIsRejected() {
        Long agentId = jdbc.queryForObject("select min(agent_id) from agent_skills", Long.class);
        Long skillId = jdbc.queryForObject("select min(skill_id) from agent_skills", Long.class);
        assertThatThrownBy(() -> jdbc.update(
            "insert into agent_skills(agent_id, skill_id) values (?,?)", agentId, skillId))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test @Transactional void auditFieldsArePopulatedAndUpdated() {
        Department item = departments.findAll().get(0);
        var created = item.getCreatedAt();
        var previous = item.getUpdatedAt();
        assertThat(created).isNotNull();
        item.setName("Changed in rollback-only test");
        em.flush();
        assertThat(item.getCreatedAt()).isEqualTo(created);
        assertThat(item.getUpdatedAt()).isAfter(previous);
    }
}
