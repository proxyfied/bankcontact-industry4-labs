# Запуск BankContact

Потрібні Java 17, Node.js 22.12+ і Docker Desktop. Команди виконуються в PowerShell із кореня проєкту.

## Корпоративний застосунок

```powershell
docker compose up -d --wait
npm --prefix frontend ci
npm --prefix frontend run build:backend
.\mvnw.cmd -DskipTests package
java -jar target/bankcontact-0.0.3-SNAPSHOT.jar --spring.profiles.active=demo
```

Адреса: http://localhost:8080. Профіль `demo` пересоздає навчальну БД і додає початкові записи. Для збереження даних запускайте без `demo`; за замовчуванням використовується `ddl-auto=update`.

Параметри підключення: `DB_URL`, `DB_USER`, `DB_PASSWORD`. Порт задає `PORT`, дозволені джерела запитів — `CORS_ALLOWED_ORIGINS`.

Для окремого запуску Vue: `npm --prefix frontend run dev`. Vite проксіює `/api` на порт 8080. `API_PROXY_TARGET` змінює адресу backend. Для прямого підключення клієнта скопіюйте `frontend/.env.example` у `frontend/.env` та налаштуйте `VITE_API_BASE_URL` і CORS.

REST API: `/api/customers`, `/api/agents`, `/api/tickets`; методи GET, POST, PUT, DELETE. [Контракт API](lab3/api.md). [Колекція Postman](../postman/BankContact_Lab3.postman_collection.json), [середовище](../postman/BankContact_Local.postman_environment.json).

## Структурування статей

Окремий модуль `ai-articles`: Spring Boot 3, Spring AI, GPT-4o-mini, Jackson і Thymeleaf.

```powershell
.\mvnw.cmd -f ai-articles/pom.xml verify
$key = Read-Host "OpenAI API key" -AsSecureString
$env:OPENAI_API_KEY = [System.Net.NetworkCredential]::new("", $key).Password
java -jar ai-articles/target/bankcontact-ai-articles-0.0.5-SNAPSHOT.jar
```

Адреса: http://localhost:8082/articles. Ключ використовується тільки сервером. Без ключа генерація повертає 503. `AI_PORT` змінює порт, `BANKCONTACT_URL` — посилання на корпоративний застосунок. `VITE_ARTICLE_AI_URL` змінює посилання в меню Vue; після зміни потрібна повторна збірка клієнта.

`POST /generate-all` приймає JSON `{"message":"Бібліографічний опис статті"}`. Відповідь містить `article` із 36 рядковими полями, `clock` у мілісекундах, `model` і `testMode`. Результат відображається у формі, у БД не зберігається. `total_volume_of_printed_sheets` за умовою роботи містить кількість сторінок; `specialized` має задане значення `Category A`.

Для звичайної генерації не задавайте тестові `OPENAI_BASE_URL` та `AI_TEST_MODE`. Помилки: 400 — невалідний запит, 415 — неправильний Content-Type, 502 — помилка моделі або відповіді, 503 — відсутній ключ.

## BI аналітика та імпорт Excel

Запустіть PostgreSQL і корпоративний застосунок за командами вище. В іншому вікні PowerShell:

```powershell
.\mvnw.cmd -f bi-service/pom.xml verify
java -jar bi-service/target/bankcontact-bi-0.0.6-SNAPSHOT.jar
```

Відкрийте http://localhost:8080/#/analytics або пункт «BI аналітика» в меню. BI-сервіс використовує Spring Boot 3 і порт 8083. Параметри БД: `DB_URL`, `DB_USER`, `DB_PASSWORD`; порт: `BI_PORT`. Для іншої адреси задайте `VITE_BI_API_URL` перед збіркою Vue і відповідний `BI_CORS_ORIGINS` на сервері.

[Файл для імпорту](../fixtures/lab6/template.xlsx) містить 12 навчальних звернень. Кнопка «Завантажити шаблон» створює файл із заголовками та одним прикладом. Замініть його власними рядками та унікальними кодами. [Формат і контракт BI](lab6/bi.md).

## Автоматичні перевірки

```powershell
.\mvnw.cmd verify
.\mvnw.cmd -f ai-articles/pom.xml verify
cd frontend
npx playwright install chromium
npm run test:e2e
```

Тести основного застосунку запускайте з окремою навчальною PostgreSQL, а браузерні — із запущеним JAR. AI-тести JUnit використовують локальний HTTP-стаб і не потребують платного API. Браузерні сценарії AI зі стабом: `npm run test:ai` з каталогу `frontend`, після запуску `ai-articles/tools/openai_stub.py` та AI-сервісу з `OPENAI_API_KEY=test-key-not-real`, `OPENAI_BASE_URL=http://127.0.0.1:8099`, `AI_TEST_MODE=true`.

Реальна перевірка запускається вручну через Actions → Lab 5 - Real GPT-4o-mini verification → Run workflow. Потрібен Actions secret `OPENAI_API_KEY`; виконується один платний запит. [Збережений результат](../evidence/lab5-live/generation-response.json), [скріншоти](../evidence/lab5-live/screenshots/). Контрольний приклад: 36 полів, 11 сторінок, `ukr`, clock 6435 мс. Це один вимір, а не середня швидкість моделі.

## Керування доступом та сесіями

```powershell
.\mvnw.cmd -f security-service/pom.xml verify
java -jar security-service/target/bankcontact-security-0.0.7-SNAPSHOT.jar --spring.profiles.active=demo
```

Адреса: http://localhost:8084/. Пункт «Доступ» у Vue веде до цього модуля. [Ролі, навчальні акаунти, параметри та перевірка сесій](lab7/security.md). Модуль використовує PostgreSQL, запущений вище; сесія захищає його сторінки на 8084.
