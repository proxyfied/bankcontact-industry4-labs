# Lab 5 — BankContact AI Articles

Окремий servlet-модуль корпоративного проєкту: Java 17, Spring Boot **3.5.15**, Spring AI **1.1.8**, GPT-4o-mini, Lombok, Jackson 2 і Thymeleaf. Основну систему ЛР3–4 на Boot 4 збережено; AI-модуль запускається на 8082 та відкривається з меню Vue.

Залежність `spring-ai-starter-model-openai` — сучасна назва наведеного у ТЗ `spring-ai-openai-spring-boot-starter`. Версії керуються BOM. Див. офіційні [upgrade notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html) та [сумісність Spring AI](https://github.com/spring-projects/spring-ai).

## Реальний запуск (Windows PowerShell із кореня репозиторію)

```powershell
.\mvnw.cmd -f ai-articles/pom.xml verify
$key = Read-Host "OpenAI API key" -AsSecureString
$env:OPENAI_API_KEY = [System.Net.NetworkCredential]::new("", $key).Password
java -jar ai-articles/target/bankcontact-ai-articles-0.0.5-SNAPSHOT.jar
```

Відкрити http://localhost:8082/articles. Без ключа UI працює, POST повертає 503. Значення `not-configured` є тільки маркером відсутньої конфігурації і не надсилається до OpenAI. Реальний ключ не потрібно вставляти в Java, properties, Vue чи Git.

`AI_PORT` змінює порт, `BANKCONTACT_URL` — зворотне посилання на корпоративний застосунок. `VITE_ARTICLE_AI_URL` налаштовує пункт меню Vue; після його зміни потрібно повторно зібрати фронтенд. За замовчуванням обидва застосунки працюють локально.

## Контракт

`POST /generate-all`, `Content-Type: application/json`:

```json
{"message":"Лозовик Ю.М. Розвиток штучного інтелекту в економіці. Журнал \"Цифрова Економіка\", Том 5, Випуск 2, 2025 рік, Київ, стор. 10-20. DOI: 10.1234/example."}
```

Відповідь містить `article` (36 полів), `clock` (тривалість серверної обробки у мс), `model` і `testMode`. Метод не зберігає результат у БД: завдання ЛР5 передбачає структурування та відображення, а не персистентність статей.

- 200 — Record сформовано.
- 400 — порожній текст, понад 12000 символів або пошкоджений JSON запиту.
- 415 — неправильний Content-Type.
- 502 — помилка провайдера, некоректна структура JSON або суперечливий результат.
- 503 — ключ на сервері не налаштовано.

`ChatControllerApi` отримує `OpenAiChatModel` через конструктор, виконує `chatModel.call(prompt)`, передає текст у Jackson-парсер та вимірює `clock`. Промпт містить окрему системну роль і `PromptTemplate` з `{message}` для цитування. Увімкнено JSON object mode. Він не гарантує відповідність усім полям; тому сервер додатково перевіряє структуру, прапорці, мову й арифметику сторінок.

Підтримуються чистий JSON або єдиний огороджений блок ```json ... ```. Зайвий текст, масиви, null-корінь, дублікати ключів, trailing JSON та об’єкт із неправильними відомими типами відхиляються. Невідомі поля ігноруються через `@JsonIgnoreProperties(ignoreUnknown = true)`.

## Схема Record

Повний вихідний зразок 30+ полів у завданні не надано. Тому використано задокументовану розширену навчальну схему з 36 рядковими полями; обов’язкові назви з ТЗ збережено. Повні визначення — у [SaveArticleToEntity.java](src/main/java/ua/edu/kneu/articles/SaveArticleToEntity.java) і [системному промпті](src/main/resources/prompts/article-system.txt).

- Бібліографія: `title_of_the_article`, `author_set`, `year_of_publication`, `name_journal`, `volume`, `issue`, `first_page`, `last_page`, `total_volume_of_printed_sheets`, `publication_type`, `publication_date`, `citation_text`, `source_text`.
- Ідентифікатори: `doi`, `issn`, `eissn`, `isbn`, `url`, `author_orcid`, `udc`.
- Географія: `publisher`, `publication_city`, `publication_country`, `language`, `published_in_ukraine`, `published_abroad`.
- Додаткові дані: `specialized`, `abstract_text`, `keywords`, `affiliation`, `references_count`, `indexed_scopus`, `indexed_web_of_science`, `peer_reviewed`, `open_access`, `notes`.

Null/порожні значення перетворюються конструктором на `""` або `"0"`. Невідомий прапорець `0` означає відсутність підтвердження, а не доведене заперечення. Мова: `ukr`, `eng`, `other` або `""`, якщо її неможливо встановити.

За умовою `total_volume_of_printed_sheets` означає **кількість сторінок**, хоча назва поля згадує друковані аркуші. Обчислення 20 − 10 + 1 = 11 доручено LLM; Java лише відхиляє суперечливу арифметику, а не підміняє її власним результатом. `specialized = "Category A"` — задана константа лабораторної, не перевірена категорія реального журналу.

## Автоматична перевірка без платного API

```powershell
.\mvnw.cmd -f ai-articles/pom.xml verify
```

JUnit перевіряє Record, prompt, JSON, правила й контролер. Інтеграційний тест використовує справжню Spring AI конфігурацію і локальний HTTP-сервер із фіксованою відповіддю OpenAI-формату; він не використовує GPT-4o-mini. Browser CI робить те саме через запущений JAR і Thymeleaf.

Для повторення browser CI локально запустити в окремих терміналах:

```powershell
python ai-articles/tools/openai_stub.py
```

```powershell
$env:OPENAI_API_KEY="test-key-not-real"
$env:OPENAI_BASE_URL="http://127.0.0.1:8099"
$env:AI_TEST_MODE="true"
java -jar ai-articles/target/bankcontact-ai-articles-0.0.5-SNAPSHOT.jar
```

```powershell
npm --prefix frontend ci
cd frontend
npx playwright install chromium
npm run test:ai
```

UI показує **ТЕСТОВИЙ РЕЖИМ**, `clock` у цьому режимі є часом інтеграції зі стабом. Це не оцінка швидкості LLM. Перед живим запуском потрібно прибрати `OPENAI_BASE_URL` і `AI_TEST_MODE` та задати справжній ключ. Немає автоматичного перемикання на фіктивну генерацію у production.

Для реального приймального тесту: запустити звичайний режим із ключем, вставити приклад, перевірити назву, `11`, `ukr`, прапорець України та заміряний clock. Додатково перевірити власне англійське і неповне цитування; не приписувати стабу якість або швидкість реальної LLM.

## Один реальний запит у GitHub Actions

1. Додати repository Actions secret `OPENAI_API_KEY` у Settings → Secrets and variables → Actions. Не створювати файл із ключем і не вставляти його в код.
2. Відкрити Actions → **Lab 5 - Real GPT-4o-mini verification** → **Run workflow** (main).
3. Workflow виконає один запит через справжній Spring AI до `https://api.openai.com`, перевірить відповідь, збереже JSON, clock та скріншоти в артефакт `lab5-real-gpt4o-mini-verification`.

Запуск тільки ручний: звичайні push не викликають платний API. Повторних спроб генерації немає. Вхідна фікстура використовується лише як цитування; відповідь береться виключно з реальної моделі. Приймальний тест перевіряє 36 рядкових полів, назву, 11 сторінок, ukr, прапорці країни та Category A. Запуск потребує дійсного ключа з доступом до моделі й доступного API-балансу/кредитів.
