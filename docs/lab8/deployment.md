# Розгортання BankContact на Linux

Обрано Docker-варіант для застосунку ЛР7: Spring Boot 3, PostgreSQL 17 і Nginx. Java запускається з профілем `prod` на внутрішньому порту 8080. Назовні публікуються тільки порти Nginx 80/443. REST/Vue, BI та AI попередніх робіт цим комплектом не розгортаються.

## Статус

Постійний VPS і домен користувача ще не надано. Комплект призначений для такого сервера; GitHub Actions перевіряє його на тимчасовому віддаленому Ubuntu runner. Це перевірка розгортання, а не постійний публічний хостинг. Сертифікат CI самопідписаний; реальний сертифікат Let’s Encrypt отримується на VPS після налаштування DNS.

## Вимоги

Ubuntu 22.04 LTS або новіша, від 1 vCPU та 2 ГБ RAM, вільні 80/443 і місце для Docker-образів та даних БД. Java-контейнер використовує Ubuntu 22.04 і JRE 17. Збірка багатоступенева: Maven із JDK 17 створює JAR, а runtime-образ містить лише JRE та застосунок.

Встановіть Docker Engine та Compose plugin за офіційною інструкцією: https://docs.docker.com/engine/install/ubuntu/. Увімкніть запуск Docker після перезавантаження: `sudo systemctl enable --now docker`.

## Підготовка VPS

Під’єднайтеся з PowerShell: `ssh ubuntu@SERVER_IP`. На сервері, якщо користувач має право запускати Docker:

```bash
sudo mkdir -p /opt/bankcontact
sudo chown "$USER":"$USER" /opt/bankcontact
git clone https://github.com/proxyfied/bankcontact-industry4-labs.git /opt/bankcontact
cd /opt/bankcontact
bash deploy/init-env.sh your-domain.example your-email@example.com
bash deploy/start-http.sh
```

Замініть домен та email власними перед виконанням. Скрипт створює приватний `deploy/.env` із двома різними випадковими паролями. Файл має режим 0600, ігнорується Git і не передається в Docker build context. Паролі не виводяться в консоль. Повторний запуск `init-env.sh` не перезаписує існуючі облікові дані.

Завжди використовуйте явний `-f docker-compose.yml`: у корені також є `compose.yml` для локальних робіт. Команди з цього документа вибирають саме production-комплект.

До налаштування HTTPS відкритий лише ACME challenge і службовий health check Nginx. Інші HTTP-адреси повертають 503; форми з паролями через незахищений HTTP не обслуговуються.

## База даних

Контейнер PostgreSQL створює `bankcontact_prod`, адміністратора `postgres` і роль застосунку `bankcontact_app`. Роль застосунку має LOGIN і власну схему `security`, але не має SUPERUSER, CREATEDB, CREATEROLE або REPLICATION. Вона може створювати та змінювати таблиці цієї схеми для Hibernate `ddl-auto=update`. Java не використовує обліковий запис адміністратора.

Дані зберігаються в named volume `pgdata`. Ініціалізація ролі виконується тільки для порожнього volume. Редагування пароля у `.env` не змінює пароль уже існуючої ролі PostgreSQL; для ротації потрібні узгоджена зміна пароля в БД і перезапуск застосунку. Не видаляйте volume заради зміни налаштувань.

Підключення зсередини контейнера:

```bash
docker compose --env-file deploy/.env -f docker-compose.yml exec db \
  psql -U bankcontact_app -d bankcontact_prod
```

Для перевірки: `SELECT current_user;` і `SELECT * FROM security.sec_roles;`. Порт 5432 на хост не публікується. SQL через локальний socket контейнера доступний адміністратору Docker, а Java підключається по внутрішній мережі з окремим паролем.

## Домен і HTTPS

Створіть DNS A-запис домену на IP VPS. AAAA додавайте тільки за налаштованого IPv6. Відкрийте порт 80 у мережевих правилах хмарного провайдера та на хості, потім:

```bash
bash deploy/enable-https.sh
```

Certbot виконає HTTP-01 через webroot. Nginx перейде на HTTPS тільки після отримання сертифіката й перевірки конфігурації. Порт 80 надалі перенаправляє на HTTPS, крім ACME challenge. Сценарій автоматично відновлює попередню конфігурацію, якщо `nginx -t` завершується помилкою.

Для автоматичного поновлення, якщо проєкт лежить у `/opt/bankcontact`:

```bash
sudo cp deploy/systemd/bankcontact-cert-renew.service /etc/systemd/system/
sudo cp deploy/systemd/bankcontact-cert-renew.timer /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now bankcontact-cert-renew.timer
sudo systemctl list-timers bankcontact-cert-renew.timer
docker compose --env-file deploy/.env -f docker-compose.yml run --rm certbot renew --dry-run
```

Якщо шлях інший, змініть WorkingDirectory та ExecStart у service-файлі. Сертифікати зберігаються в ігнорованому Git каталозі `deploy/runtime/letsencrypt`. `deploy/ci-cert.sh` створює лише тестовий самопідписаний сертифікат і не використовується для публічного VPS.

## Мережа та firewall

Nginx передає X-Forwarded-For, X-Real-IP, X-Forwarded-Proto, X-Forwarded-Host та X-Forwarded-Port. Клієнтські значення цих forwarded-заголовків перезаписуються. Spring Boot використовує native RemoteIpValve; приватні проксі Docker потрапляють до стандартного списку довірених адрес Tomcat. Порти Java та БД не мають host publication і знаходяться у внутрішніх Docker-мережах.

Перевірте на сервері поточні правила `sudo ufw status numbered`. Для стандартного SSH на 22:

```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
sudo ufw status verbose
```

Перед увімкненням перевірте фактичний SSH-порт і наявні правила. Видаліть непотрібні попередні дозволи через `ufw delete`. Так само обмежте inbound правила security group провайдера до 22/80/443. Docker може обходити UFW для опублікованих портів, тому Java і PostgreSQL взагалі не публікуються, а не покладаються лише на UFW. Джерело: https://docs.docker.com/engine/install/ubuntu/#firewall-limitations.

## Облікові записи та сесії

У prod немає стандартних demo-акаунтів. Сумісне ввімкнення `prod,demo` зупиняє застосунок до створення таких акаунтів. Зареєструйте власний акаунт через HTTPS. За потреби власник сервера може надати роль:

```bash
bash deploy/assign-role.sh your.username ROLE_ADMIN
```

Після зміни ролі вийдіть та увійдіть знову. Це локальна адміністративна команда сервера, а не публічний endpoint.

Production cookie — `JSESSIONID`, з HttpOnly, Secure і SameSite=Lax. Logout видаляє cookie з налаштованим ім’ям. Одночасно дозволена одна активна сесія; вхід з другого браузера витісняє перший. Тайм-аут бездіяльності — 20 хв. Сесії зберігаються в пам’яті одного Java-процесу: після перезапуску потрібен повторний вхід, тоді як користувачі й ролі залишаються в PostgreSQL. Для кількох екземплярів потрібна окрема реалізація Spring Session Redis; вона тут не налаштована.

## Експлуатація

```bash
docker compose --env-file deploy/.env -f docker-compose.yml ps
docker compose --env-file deploy/.env -f docker-compose.yml logs --tail=100 -f app
curl --fail https://your-domain.example/actuator/health/readiness
bash deploy/backup.sh
```

Actuator відкриває тільки health, без подробиць БД чи середовища. Контейнери мають health checks та `restart: unless-stopped`; профіль prod завершує Java коректно. Логи йдуть у stdout і ротуються Docker: до трьох файлів по 10 МБ на контейнер. Резервні копії зберігаються приватно в `deploy/backups` і не потрапляють у Git. Переносьте резервні копії також за межі VPS.

Оновлення: створіть backup, виконайте `git pull --ff-only`, потім `docker compose --env-file deploy/.env -f docker-compose.yml up -d --build --wait`. БД не видаляється. Після оновлення перевірте health і вхід. Відкат образу не скасовує зміни схеми Hibernate; відновлення БД виконується тільки з перевіреної резервної копії.

## Перевірка в CI

Workflow `verify-deployment.yml` використовує Ubuntu 24.04, збирає реальний Docker-образ, запускає PostgreSQL, Java та Nginx. Для перевірки використовуються порти 8088/8443 і самопідписаний тестовий сертифікат. Curl перевіряє сертифікат із явно заданим тестовим CA. Перевіряються HTTPS, JSESSIONID, CSRF, RBAC, одна сесія, перезапуски Java/БД, збереження акаунту, відсутність зовнішніх портів 8080/5432 і заборона CREATE DATABASE для користувача застосунку.

Контейнери CI тимчасові й зупиняються наприкінці workflow. Наявність успішної перевірки не означає, що створено постійний VPS, виконано його reboot, налаштовано firewall хмарного акаунту або видано публічний сертифікат Let’s Encrypt.
