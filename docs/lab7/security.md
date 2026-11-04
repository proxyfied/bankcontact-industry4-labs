# Керування доступом BankContact

`security-service` — модуль Spring Boot 3.5.15, Spring Security 6, JPA, PostgreSQL, Thymeleaf і Lombok на Java 17. Працює на порту 8084; пункт «Доступ» у меню Vue відкриває його головну сторінку.

## Запуск

Із кореня проєкту, PowerShell, після запуску PostgreSQL через `docker compose up -d --wait`:

```powershell
.\mvnw.cmd -f security-service/pom.xml verify
java -jar security-service/target/bankcontact-security-0.0.7-SNAPSHOT.jar --spring.profiles.active=demo
```

Відкрити http://localhost:8084/. Модуль використовує `DB_URL`, `DB_USER`, `DB_PASSWORD`, як інші сервіси. Порт змінює `SECURITY_PORT`, посилання на корпоративний застосунок — `BANKCONTACT_URL`. `VITE_SECURITY_URL` задає адресу пункту меню Vue перед його збіркою.

## Навчальні акаунти

Профіль `demo` створює відсутні навчальні акаунти; наявні паролі не скидає. Без цього профілю створюються лише три довідникові ролі, а користувач реєструється через форму.

| Логін | Навчальний пароль | Роль |
|---|---|---|
| demo.user | UserDemo!2026 | ROLE_USER |
| demo.manager | ManagerDemo!2026 | ROLE_MANAGER |
| demo.admin | AdminDemo!2026 | ROLE_ADMIN |

Це відкриті локальні демонстраційні облікові записи. Для іншого навчального середовища задайте `DEMO_USER_PASSWORD`, `DEMO_MANAGER_PASSWORD`, `DEMO_ADMIN_PASSWORD` до першого запуску `demo`. За HTTPS задайте `COOKIE_SECURE=true`; локально використовується HTTP.

## Модель доступу

| Шлях | Анонім | USER | MANAGER | ADMIN |
|---|---|---|---|---|
| / | 200 | 200 | 200 | 200 |
| /login, /registration | Дозволено | 403 | 403 | 403 |
| /user, /user/** | Перехід на /login | Дозволено | Дозволено | Дозволено |
| /manager, /manager/** | Перехід на /login | 403 | Дозволено | 403 |
| /admin, /admin/** | Перехід на /login | 403 | 403 | Дозволено |

Кореневі сторінки `/user`, `/manager`, `/admin` та їхні варіанти з кінцевою рискою мають Thymeleaf views. Правила `/**` захищають вкладені шляхи; непередбачені контролером адреси не створюють додаткових сторінок. ADMIN не успадковує MANAGER.

## Дані та реєстрація

`User` і `Role` зберігаються в `sec_users` та `sec_roles`, зв’язок Many-to-Many — у `sec_user_roles`. Унікальні username/name та зовнішні ключі забезпечує PostgreSQL. UserService імплементує UserDetailsService, завантажує ролі через EntityGraph і перетворює їх на GrantedAuthority. Стандартний principal Spring Security порівнює користувачів за логіном, що потрібно для обмеження сесій.

Логіни нечутливі до регістру: зберігаються малими латинськими літерами. Дозволено 3–40 літер, цифр і символів `._-`. Пароль має не менше 8 символів і не більше 72 байтів UTF-8; підтвердження має збігатися. BCryptPasswordEncoder використовує cost 12 та випадкову сіль. Пароль не повертається у форму після помилки.

Публічна реєстрація приймає лише username, password, confirmation. Призначення ролі зафіксовано в сервісі як ROLE_USER; передані клієнтом roles не дають додаткових прав. Для навчальних ролей ADMIN/MANAGER використовується тільки явно активований demo-профіль. Публічного API підвищення ролі немає.

## Сесії

SecurityFilterChain використовує formLogin, POST logout та стандартний CSRF-захист. Thymeleaf автоматично додає токен у POST-форми. Після входу відкривається /user, після виходу — /. Захист від фіксації сесії змінює її id під час входу.

`maximumSessions(1)` і `maxSessionsPreventsLogin(false)` дозволяють другий вхід та позначають попередню сесію простроченою. Під час наступного запиту зі старого браузера ConcurrentSessionFilter завершує її й перенаправляє на `/login?expired`. Новий браузер продовжує працювати. SessionRegistryImpl відстежує входи; HttpSessionEventPublisher видаляє завершені сесії. Панель /admin показує лише активні логіни й кількість сесій, без їхніх ідентифікаторів.

Cookie `BANKCONTACT_AUTH` має HttpOnly та SameSite=Lax; вона відокремлена за ім’ям від JSESSIONID інших модулів. Тайм-аут бездіяльності — 20 хвилин. Реєстр зберігається в пам’яті одного екземпляра сервісу; обмеження в цій роботі перевіряється для цього екземпляра.

## Межі модуля

Авторизація застосовується до сторінок `security-service` на 8084. Раніше створені REST, BI та AI-сервіси мають власні порти й не використовують цю сесію; спільний SSO або gateway в цій роботі не реалізовано. Сторінка менеджера демонструє доступ за роллю, а не новий модуль планування задач.

## Перевірка

JUnit перевіряє реєстрацію, BCrypt, валідацію, відсутність підвищення ролі, матрицю доступу, CSRF, logout та витіснення першої сесії. Локально використовується тестова H2; workflow `verify-security.yml` повторює ці тести на PostgreSQL 17 через `TEST_DB_URL`, `TEST_DB_USER`, `TEST_DB_PASSWORD`.

Playwright працює зі справжнім JAR і PostgreSQL. Для перевірки одночасних входів створює два ізольовані контексти браузера з різними cookie-сховищами. Це перевірка двох незалежних браузерних сесій у Chromium. Платні AI-запити не виконуються.

Джерела: https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/session-management.html та https://docs.spring.io/spring-security/reference/6.5/features/authentication/password-storage.html.
