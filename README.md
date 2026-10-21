# Campus Bornholm – backend

REST API til studerende på Campus Bornholm: ugens menu, events, nyheder og Wordle.
Bygget med Javalin, Hibernate/JPA og PostgreSQL.

## Kom i gang

1. Start Docker Desktop.
2. Start databasen:
   ```
   docker compose up -d
   ```
3. Kopiér `config.properties.example` til `src/main/resources/config.properties`
   (findes allerede lokalt – filen er i `.gitignore`).
4. Kør `Main` i IntelliJ.
5. Åbn `requests.http` og kør `GET /api/hello`.

Alle endpoints: http://localhost:7070/api/routes

## Tests

```
mvn test
```
`HibernateConfigTest` bruger Testcontainers og kræver at Docker kører – ellers springes den over.

## Struktur

```
dk.campusbornholm
├── config/        HibernateConfig, ApplicationConfig, Routes, AppProperties
├── entities/      User, Role, News, Event, Dish, Word, WordleGame, Guess
├── dtos/          det der sendes ind/ud via API'et
├── daos/          databaseadgang
├── controllers/   håndterer requests
├── services/      forretningslogik (Wordle, scraper)
├── security/      JWT og roller
└── exceptions/    ApiException
```
