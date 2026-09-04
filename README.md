# PoC Modular Monolith

A Spring Boot 4 modular monolith boilerplate demonstrating hexagonal (ports & adapters) architecture with Keycloak, Vault, and multi-schema Flyway.

## Tech Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| Java | 25 | Runtime |
| Spring Boot | 4.0.0 | Framework |
| PostgreSQL | 16 | Primary database |
| PgBouncer | latest | Connection pooling |
| Keycloak | latest | Identity provider (OIDC) |
| Vault | latest | Secrets management |
| Jaeger | latest | Distributed tracing |
| Loki | latest | Log storage |
| Grafana | latest | Observability UI |

## Quick Start

### Prerequisites
- Java 25
- Maven 3.9+
- Docker & Docker Compose

### 1. Start infrastructure
```bash
make infra-up
```

### 2. Wait for Keycloak to be ready
```bash
docker compose logs -f keycloak | grep "Running the server"
```

### 3. Initialize Vault
```bash
make vault-init
```

### 4. Install modules (first time only)
```bash
make install
```

### 5. Run the application
```bash
make run
```

The app starts at http://localhost:8080.

### 6. Test the API
```bash
# Get a token (requires Keycloak realm setup — see below)
make token

# List users (requires Bearer token in production mode)
curl http://localhost:8080/api/users
```

## Architecture

### Module Dependency Graph

```
poc-common          (shared kernel — base classes, context, events)
    ↑ compile
    ├── poc-user       (User module + Keycloak integration)
    └── poc-notification (Notification module — depends on poc-user for UserPublicApi)
              ↑
           poc-app      (assembles everything — filters, interceptors, config, migrations)
```

### Request Flow

```
Client → Gateway (Kong/Envoy)
           │ validates JWT, injects X-User-* headers
           ↓
         poc-app (:8080)
           │ UserContextFilter reads X-User-* → ThreadLocal
           │ PermissionInterceptor checks @RequirePermission / @RequireRole
           ↓
         Controller → Application Service → Domain → SPI → JPA Adapter
           ↓
         PgBouncer (:6432) → PostgreSQL (:5432)
```

### Hexagonal Layers

Each module follows this structure:

```
poc-{module}/src/main/java/com/poc/{module}/
├── domain/
│   ├── model/          # Aggregate root (extends AggregateRoot<String>)
│   └── spi/            # Outbound ports (repository interfaces)
├── application/
│   ├── api/            # Inbound ports (use case interfaces)
│   └── service/        # Use case implementations (@Service)
├── infrastructure/
│   ├── inbound/web/    # REST controllers + DTOs
│   ├── outbound/       # JPA entities, repositories, adapters
│   └── config/         # @Configuration + @ComponentScan
└── publicapi/          # Public API for other modules
```

### Key Rules
- **Domain objects** have NO JPA annotations
- **SPIs** return domain objects, not JPA entities
- **Only `publicapi/`** is visible to other modules
- **Each module** has its own Postgres schema
- **Events** in `poc-common` enable cross-module communication without compile dependencies

## Integrations

### Keycloak (Identity Provider)

The user module integrates with Keycloak for user management:

- `IdentityProviderPort` — domain SPI for identity operations
- `KeycloakUserAdapter` — infrastructure adapter calling Keycloak REST API
- `RegisterUserService` — orchestrates: create Keycloak user → save to DB → publish event
- If DB save fails, rolls back by deleting the Keycloak user

**Keycloak config** in `application.yml`:
```yaml
keycloak:
  auth-server-url: http://localhost:8081
  realm: poc
  client-id: poc-api
  admin-username: admin
  admin-password: admin
```

### Vault (Secrets Management)

Vault stores database credentials. The app reads them at startup via Spring Cloud Vault:

```yaml
spring:
  config:
    import: vault://
  cloud:
    vault:
      host: localhost
      port: 8200
      authentication: TOKEN
      token: root-token
      kv:
        enabled: true
        backend: secret
        default-context: poc
```

**Initialize Vault:**
```bash
make vault-init
```

### Flyway Multi-Schema

Each module gets its own Postgres schema. Flyway manages all from one migration directory:

```yaml
spring:
  flyway:
    schemas:
      - poc_user
      - poc_notification
    create-schemas: true
```

**Adding a new schema:**
1. Create migration `V{N}__create_{name}_tables.sql` in `poc-app/src/main/resources/db/migration/`
2. Add `poc_{name}` to `spring.flyway.schemas` in `application.yml`
3. Use `@Table(schema = "poc_{name}")` on JPA entity

## How to Add a New Module

### Step-by-step

1. **Create directory:**
   ```bash
   mkdir -p poc-{name}/src/main/java/com/poc/{name}/{domain/{model,spi},application/{api,service},infrastructure/{inbound/web,outbound/persistence,config},publicapi}
   ```

2. **Create `poc-{name}/pom.xml`** — parent is `poc-parent`, depends on `poc-common`

3. **Add to parent `pom.xml`** — `<module>poc-{name}</module>`

4. **Add to `poc-app/pom.xml`** — `<dependency>poc-{name}</dependency>`

5. **Create domain model** — extends `AggregateRoot<String>`, has `getId()`, `equals()`, `hashCode()`

6. **Create SPI** — interface in `domain/spi/`, returns domain objects

7. **Create use cases** — interfaces in `application/api/`

8. **Create services** — `@Service` with constructor-injected SPIs

9. **Create JPA entity** — `@Entity` + `@Table(schema = "poc_{name}")`

10. **Create adapter** — `@Component`, implements SPI, maps domain ↔ JPA

11. **Create controller** — `@RestController`, `@RequirePermission` on endpoints

12. **Create module config** — `@Configuration` + `@ComponentScan(basePackages = "com.poc.{name}")`

13. **Create Flyway migration** — `V{N}__create_{name}_tables.sql` in `poc-app/`

14. **Add schema to `application.yml`** — add `poc_{name}` to `spring.flyway.schemas`

### Checklist

- [ ] Domain model extends `AggregateRoot<String>`
- [ ] SPI returns domain objects, not JPA entities
- [ ] Service is `@Service` with constructor injection
- [ ] JPA entity has `@Table(schema = "poc_{name}")`
- [ ] Controller has `@RequirePermission` on every endpoint
- [ ] Module config has `@ComponentScan`
- [ ] Flyway migration creates schema + tables
- [ ] Schema in `spring.flyway.schemas`
- [ ] `mvn clean compile -DskipTests` passes

## Makefile Commands

| Command | Description |
|---------|-------------|
| `make help` | Show all available commands |
| `make infra-up` | Start all infrastructure |
| `make infra-down` | Stop infrastructure |
| `make infra-reset` | Stop + delete volumes (reset DB) |
| `make build` | Build all modules |
| `make install` | Install all modules (first time) |
| `make run` | Run the application |
| `make run-dev` | Run with dev profile (Swagger) |
| `make test` | Run all tests |
| `make clean` | Clean build artifacts |
| `make status` | Show Docker Compose status |
| `make logs` | Tail infrastructure logs |
| `make vault-init` | Initialize Vault with DB credentials |
| `make token` | Get an access token from Keycloak |

## Infrastructure Ports

| Service | Port | Notes |
|---------|------|-------|
| App | 8080 | Spring Boot |
| PostgreSQL | 5432 | Direct access |
| PgBouncer | 6432 | Pooled access |
| Keycloak | 8081 | Admin console (admin/admin) |
| Vault | 8200 | Secrets API/UI (root-token) |
| Jaeger | 16686 | Tracing UI |
| Grafana | 3000 | Observability UI (admin/admin) |
| Loki | 3100 | Log storage API |
