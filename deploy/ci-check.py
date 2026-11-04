import json, socket, subprocess
from pathlib import Path

root = Path(__file__).resolve().parents[1]
out = root / 'evidence/lab8'
out.mkdir(parents=True, exist_ok=True)
compose = ['docker', 'compose', '--env-file', 'deploy/.env', '-f', 'docker-compose.yml']

def run(args, check=True):
    return subprocess.run(args, cwd=root, capture_output=True, text=True, check=check)

network = {}
for service in ['app', 'db']:
    container = run(compose + ['ps', '-q', service]).stdout.strip()
    ports = json.loads(run(['docker', 'inspect', '--format', '{{json .NetworkSettings.Ports}}', container]).stdout)
    assert not any(ports.values()), f'{service} unexpectedly publishes a port'
    network[service] = ports
for port in [8080, 5432]:
    try:
        with socket.create_connection(('127.0.0.1', port), timeout=2):
            raise AssertionError(f'Port {port} is reachable')
    except OSError:
        pass
network['host8080Closed'] = True
network['host5432Closed'] = True
uid = run(compose + ['exec', '-T', 'app', 'id', '-u']).stdout.strip()
assert uid == '10001'
network['appUid'] = uid
(out / 'network-check.json').write_text(json.dumps(network, indent=2))

sql = """SELECT current_user, rolsuper, rolcreatedb, rolcreaterole
FROM pg_roles WHERE rolname=current_user;
SELECT u.username, left(u.password,7)=chr(36)||'2a'||chr(36)||'12'||chr(36) AS bcrypt_cost_12,
string_agg(r.name,', ' ORDER BY r.name) AS roles FROM security.sec_users u
JOIN security.sec_user_roles ur ON ur.user_id=u.id JOIN security.sec_roles r ON r.id=ur.role_id
GROUP BY u.username ORDER BY u.username;"""
result = run(compose + ['exec', '-T', 'db', 'psql', '-U', 'bankcontact_app', '-d', 'bankcontact_prod', '-c', sql])
denied = run(compose + ['exec', '-T', 'db', 'psql', '-U', 'bankcontact_app', '-d', 'bankcontact_prod', '-c', 'CREATE DATABASE forbidden_lab8;'], check=False)
assert denied.returncode != 0 and 'permission denied' in denied.stderr.lower()
(out / 'database-check.txt').write_text(result.stdout + '\nCREATE DATABASE by app role: permission denied (expected).\n')
schema = run(compose + ['exec', '-T', 'db', 'pg_dump', '-U', 'postgres', '-d', 'bankcontact_prod', '--schema-only', '--schema=security'])
(out / 'schema.sql').write_text(schema.stdout)
versions = run(['docker', 'version', '--format', '{{.Server.Version}}']).stdout
versions += run(compose + ['exec', '-T', 'app', 'java', '-version']).stderr
versions += run(compose + ['exec', '-T', 'app', 'cat', '/etc/os-release']).stdout
versions += run(compose + ['exec', '-T', 'db', 'postgres', '--version']).stdout
versions += run(compose + ['exec', '-T', 'nginx', 'nginx', '-v']).stderr
(out / 'versions.txt').write_text(versions)
print('Network isolation, non-root Java, limited database role and persisted data verified.')
