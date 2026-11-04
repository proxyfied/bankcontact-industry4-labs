# BankContact Vue 3 client — Lab 4

Vue 3 (Composition API), Vite, axios, Vue Router 4, Bootstrap 5.

## Development (Windows PowerShell, repository root)

Start PostgreSQL and Spring Boot as described in the repository README. In another terminal:

```powershell
cd frontend
npm ci
npm run dev
```

Open the Local URL printed by Vite, normally http://localhost:5173. Vite proxies `/api` to http://localhost:8080. The `API_PROXY_TARGET` environment variable overrides that target.

## One integrated JAR

```powershell
npm --prefix frontend ci
npm --prefix frontend run build:backend
.\mvnw.cmd -DskipTests package
java -jar target/bankcontact-0.0.3-SNAPSHOT.jar --spring.profiles.active=demo
```

Open http://localhost:8080. `build:backend` copies the actual Vite build into Spring Boot static resources. These generated files are ignored in Git; rebuild them before packaging. The demo profile resets the learning database and creates synthetic test records; use it only for a separate laboratory database.

Hash navigation supports `/#/customers` and `/#/about` directly, including reload, without a server-side route fallback. The same-origin build uses `/api`. To access the backend directly from a separate frontend origin, copy `.env.example` to `.env`, set `VITE_API_BASE_URL=http://localhost:8080/api`, and ensure the backend `CORS_ALLOWED_ORIGINS` includes the exact frontend origin. Vite reads `.env` at startup/build time.

## Browser verification

With the integrated JAR running:

```powershell
cd frontend
npx playwright install chromium
npm run test:e2e
```

Seven sequential Playwright tests run against the real API and database. They verify three pages and SPA navigation, CRUD and persistence after reload, cancellation, duplicate-code/history conflicts, a deliberately aborted HTTP request and retry, mobile layout, server-side pagination, and required/whitespace-only fields. Test-created records are cleaned up. Run only against a disposable demo database.

Screenshots and a JSON report are written to `evidence/lab4/`. The network-failure scenario intentionally aborts a browser request; all successful CRUD and pagination scenarios use real Spring Boot and PostgreSQL.
