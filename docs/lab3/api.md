# Контракт BankContact API

GET повертає JSON DTO або сторінку. POST та PUT приймають JSON. DELETE повертає 204 без тіла.

| Метод | URL | Результат |
|---|---|---|
| GET | /api/customers?page=0&size=20 | 200 PageResponse |
| GET | /api/customers/{id} | 200 DTO |
| POST | /api/customers | 201 DTO + Location |
| PUT | /api/customers/{id} | 200 DTO |
| DELETE | /api/customers/{id} | 204 |
| GET | /api/agents?page=0&size=20 | 200 PageResponse |
| GET | /api/agents/{id} | 200 DTO |
| POST | /api/agents | 201 DTO + Location |
| PUT | /api/agents/{id} | 200 DTO |
| DELETE | /api/agents/{id} | 204 |
| GET | /api/tickets?page=0&size=20 | 200 PageResponse |
| GET | /api/tickets/{id} | 200 DTO |
| POST | /api/tickets | 201 DTO + Location |
| PUT | /api/tickets/{id} | 200 DTO |
| DELETE | /api/tickets/{id} | 204 |

Обов’язкові поля запитів описано в src/main/java/ua/edu/kneu/bankcontact/dto/*Request.java.

У запитах зв’язки задаються через id; JPA-об’єкти безпосередньо у JSON не передаються. Для списків page ≥ 0, 1 ≤ size ≤ 100. PUT — повна заміна значень, а не PATCH. Помилки 400/404/409/415 повертаються як application/problem+json.
