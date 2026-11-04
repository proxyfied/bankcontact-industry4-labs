-- Run in pgAdmin Query Tool on bankcontact_lab2 after the demo run.
SELECT version();
SELECT 'departments' AS table_name, COUNT(*) FROM departments
UNION ALL SELECT 'agents', COUNT(*) FROM agents
UNION ALL SELECT 'skills', COUNT(*) FROM skills
UNION ALL SELECT 'customers', COUNT(*) FROM customers
UNION ALL SELECT 'tickets', COUNT(*) FROM tickets
UNION ALL SELECT 'interactions', COUNT(*) FROM interactions
UNION ALL SELECT 'agent_skills', COUNT(*) FROM agent_skills ORDER BY 1;

SELECT conrelid::regclass AS table_name, conname, contype,
       pg_get_constraintdef(oid) AS definition
FROM pg_constraint WHERE connamespace = 'public'::regnamespace
ORDER BY conrelid::regclass::text, conname;

SELECT tablename, indexname, indexdef FROM pg_indexes
WHERE schemaname = 'public' ORDER BY tablename, indexname;

SELECT a.employee_code, s.code AS skill_code
FROM agents a JOIN agent_skills x ON x.agent_id = a.id
JOIN skills s ON s.id = x.skill_id ORDER BY 1, 2;

SELECT t.ticket_code, c.external_code, a.employee_code,
       i.event_code, i.channel, i.occurred_at
FROM tickets t JOIN customers c ON c.id = t.customer_id
LEFT JOIN agents a ON a.id = t.assigned_agent_id
LEFT JOIN interactions i ON i.ticket_id = t.id ORDER BY t.ticket_code;
