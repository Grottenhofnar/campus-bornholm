# Campus Bornholm – backend

REST API til studerende på Campus Bornholm: ugens menu, events og login.
Bygget med Javalin 6, Hibernate/JPA og PostgreSQL. Menu og aktiviteter hentes fra campusnews.showcode.dk.

## Kom i gang

Projektet bruger kursets fælles Postgres-container `db` (`postgres:16.2` på port 5432).

1. Start Docker Desktop, og sørg for at containeren `db` kører: `docker start db`
2. Opret databasen én gang:
   ```
   docker exec -it db psql -U postgres -c "CREATE DATABASE campus_bornholm;"
   ```
3. Kopiér `config.properties.example` til `src/main/resources/config.properties` og udfyld værdierne
   (`SECRET_KEY` skal være mindst 32 tegn).
4. Kør `Main` i IntelliJ – Hibernate opretter tabellerne, admin-brugeren oprettes, og menu/events importeres.
   Serveren kører på http://localhost:7070/api
5. Brug `requests.http` til at prøve endpoints.

Nulstil databasen (stop `Main` først):

```
docker exec -it db psql -U postgres -c "DROP DATABASE campus_bornholm WITH (FORCE);"
docker exec -it db psql -U postgres -c "CREATE DATABASE campus_bornholm;"
```

Alle endpoints og roller: `GET /api/routes`

## Konfiguration

| Nøgle | Eksempel | Bruges til |
|---|---|---|
| `CONNECTION_STR` | `jdbc:postgresql://localhost:5432/` | Database-URL uden databasenavn |
| `DB_NAME` | `campus_bornholm` | Databasenavn |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` / `postgres` | Database-login |
| `ISSUER` | `Campus Bornholm` | Udsteder af JWT |
| `TOKEN_EXPIRE_TIME` | `86400000` | Tokenets levetid i ms (24 timer) |
| `SECRET_KEY` | 32+ tegn | Signering af JWT |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | `admin` / … | Admin-bruger der oprettes ved opstart |

Lokalt læses de fra `config.properties`. På serveren sættes de som miljøvariabler, som vinder over filen.

## API-dokumentation

Base-URL: `http://localhost:7070/api`. Beskyttede endpoints kræver headeren `Authorization: Bearer <token>`.

Alle fejl har samme format:

```json
{ "status": 404, "msg": "Event med id 99 findes ikke", "timestamp": "2026-10-26T21:30:00" }
```

### Auth

| Method | URL | Request Body (JSON) | Response (JSON) | Error (e) |
|---|---|---|---|---|
| GET | `/auth/healthcheck` | | `{"msg": "API is up and running"}` | |
| POST | `/auth/register` | `{"username": String, "password": String}` | (e1) `201` | (e2) 400, (e3) 409 |
| POST | `/auth/login` | `{"username": String, "password": String}` | (e1) `200` | (e2) 400, (e4) 401 |
| GET | `/auth/me` | | `{"username": String, "roles": [String]}` | (e5) 401 |

### Events

| Method | URL | Request Body (JSON) | Response (JSON) | Error (e) |
|---|---|---|---|---|
| GET | `/events?days=14` | | `[event, ...]` (v1), de næste `days` dage (standard 14, max 365) | (e2) 400 |
| GET | `/events/{id}` | | `event` (v1) | (e6) 404 |
| POST | `/events` | `{"title": String, "description"?: String, "location"?: String, "startTime": "2026-10-30T16:00:00", "endTime"?: "..."}` | `event` (v1) `201` | (e2) 400, (e5) 401, (e7) 403 |
| PATCH | `/events/{id}` | samme felter som POST, alle valgfrie | `event` (v1) | (e2) 400, (e5) 401, (e7) 403, (e6) 404 |
| DELETE | `/events/{id}` | | `204` | (e5) 401, (e7) 403, (e6) 404 |

### Menu

| Method | URL | Request Body (JSON) | Response (JSON) | Error (e) |
|---|---|---|---|---|
| GET | `/menu/today` | | `[dish, ...]` (d1) | |
| GET | `/menu/week` | | `{"weekNumber": 44, "from": "2026-10-26", "to": "2026-10-30", "dishes": [dish, ...]}` (d1) | |

### Roller

| Rolle | Adgang |
|---|---|
| ANYONE | `/hello`, `/auth/healthcheck`, `/auth/register`, `/auth/login`, alle `GET` på events og menu |
| USER | `/auth/me` |
| ADMIN | `POST`, `PATCH`, `DELETE` på events |

### Response-formater

```
(e1) Token:  { "token": String, "username": String, "roles": [String] }

(v1) Event:  { "id": Number, "title": String, "description": String, "location": String,
               "startTime": "2026-10-30T16:00:00", "endTime": "2026-10-30T22:00:00",
               "createdBy": String | null }

(d1) Dish:   { "id": Number, "name": String, "description": String, "price": Number,
               "date": "2026-10-26", "sourceUrl": String }
```

### Fejl

| | Status | Eksempel på `msg` |
|---|---|---|
| (e2) | 400 | `title skal udfyldes` / `Body er ikke gyldig JSON eller har forkerte typer` |
| (e3) | 409 | `Brugernavnet 'elias' er optaget` |
| (e4) | 401 | `Forkert brugernavn eller password` |
| (e5) | 401 | `Authorization-header mangler` / `Ugyldigt eller udløbet token` |
| (e6) | 404 | `Event med id 99 findes ikke` |
| (e7) | 403 | `Kræver en af rollerne [ADMIN], men brugeren har [USER]` |

## Deployment

Push til `master` → GitHub Actions bygger og tester → image pushes til Docker Hub (`<bruger>/campus_bornholm_api:latest`) → Watchtower på dropletten henter det nye image inden for 5 minutter.

Filerne til dropletten ligger i `deployment/`:

| Fil | Indhold |
|---|---|
| `docker-compose.yml` | `db` (Postgres 16.2), `campusAPI`, `caddy` (HTTPS) og `watchtower` |
| `Caddyfile` | Sender `https://$DOMAIN` videre til `campusAPI:7070` |
| `.env.example` | Skabelon til `.env` med domæne, Docker Hub-navn og hemmeligheder. Den rigtige `.env` findes kun på serveren |

På dropletten (som brugeren `jetty`):

```
mkdir -p ~/deployment && cd ~/deployment
nano docker-compose.yml   # indhold fra deployment/docker-compose.yml
nano Caddyfile            # indhold fra deployment/Caddyfile
nano .env                 # udfyld ud fra deployment/.env.example
docker compose up -d
docker ps
```

Tjek: `https://<DOMAIN>/api/auth/healthcheck`

## Tests

```
mvn test
```

Én testklasse af hver slags:

| Klasse | Type | Database |
|---|---|---|
| `CampusApiServiceTest` | Unit test af mapping fra campus-API'et | Nej |
| `EventDAOTest` | DAO-integrationstest | Testcontainers (`HibernateTestConfig`) |
| `EventControllerTest` | RestAssured inkl. login, 401 og 403 | Testcontainers |

Docker skal køre, ellers springes database-testene over.

## Struktur

```
dk.campusbornholm
├── config/        AppProperties, AppClock, ApplicationConfig, HibernateConfig, Routes
├── controllers/   EventController, MenuController
├── daos/          IDAO, EventDAO, DishDAO, UserDAO
├── dtos/          records der sendes ind/ud (external/ = campusnews-API'et)
├── entities/      User, Role, Event, Dish
├── exceptions/    ApiException
├── security/      SecurityController, SecurityRoutes, ISecurityDAO, ISecurityUser
└── services/      CampusApiService, CampusImportService
```
