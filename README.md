# PoC Modular Monolith

A Spring Boot 4 modular monolith boilerplate demonstrating Clean Architecture with Kong API Gateway, Keycloak, Vault, and multi-schema Flyway.

## Tech Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| Java | 25 | Runtime |
| Spring Boot | 4.0.0 | Framework |
| Kong | latest | API Gateway (DB-less mode) |
| PostgreSQL | 16 | Primary database (master + streaming replica) |
| PgBouncer | latest | Connection pooling (separate instances for master/replica) |
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
This starts PostgreSQL (master + replica), PgBouncer x2, Keycloak (auto-initializes realm/roles), Kong (auto-loads JWT config), Vault, Jaeger, Loki, and Grafana.

### 2. Install modules (first time only)
```bash
make install
```

### 3. Run the application
```bash
make run-dev
```
This runs with the `dev` profile which loads local Vault config automatically.

### 4. Test the API via Kong
```bash
# Get a token
make token

# Test via Kong gateway (port 8000)
make test-api
```

## IntelliJ IDEA Setup

### Import Project

1. **File → Open** → Select the root `pom.xml`
2. Choose **"Open as Project"**
3. Wait for Maven to index all modules

### Start Infrastructure

```bash
make infra-up      # PostgreSQL, Keycloak, Vault, etc.
make kafka-up      # Kafka, MailHog
```

### Run Configurations

Create these in **Run → Edit Configurations**:

| Name | Main Class | Module | Profile | VM Options |
|------|-----------|--------|---------|------------|
| API Server | `com.poc.app.PocApplication` | poc-app | dev | — |
| Email Worker | `com.poc.worker.WorkerApplication` | poc-worker | dev | `--worker.handlers=email` |

### Alternative: Quick Run

1. Open `poc-app/src/main/java/com/poc/app/PocApplication.java`
2. Click the **green play button** → `Run 'PocApplication'`
3. Set **Active profiles**: `dev`

### Dev Profile Features

- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **Local Vault token**: Auto-loaded from `.vault-token`
- **No auth required**: Test directly on port 8080

### Quick Test

```bash
make test-api-direct   # Test without auth (port 8080)
```

### Key Points

- **API Server** runs on port `8080`
- **Worker** has no web server (background process)
- **All modules** compile together — no need to run `mvn install` first
- **Database routing** is automatic: writes → master, reads → replica

## Architecture

### Request Flow

```
Client → Kong (:8000)
           │ 1. JWT plugin validates RS256 signature (Keycloak's public key)
           │ 2. Lua post-function parses JWT payload
           │ 3. Checks resource_access.poc-api.roles vs route allow-list
           │ 4. If missing → 403 Forbidden
           │ 5. Strips Authorization header
           │ 6. Injects X-User-Id, X-User-Roles, X-User-Permissions, X-User-Name
           ↓
         Spring Boot (:8080)
           │ UserContextFilter reads X-User-* → ThreadLocal
           │ PermissionInterceptor checks @RequirePermission / @RequireRole
           ↓
         Controller → Application Service → Domain → SPI → JPA Adapter
           ↓
         RoutingDataSource
           ├── @Transactional            → primary-pool → PgBouncer (:6432) → PostgreSQL master (:5432)
           └── @Transactional(readOnly)  → replica-pool → PgBouncer-replica (:6433) → PostgreSQL replica
```

### Module Dependency Graph

```
poc-common          (shared kernel — base classes, context, events)
    ↑ compile
    ├── poc-user       (User module + Keycloak integration)
    └── poc-notification (Notification module — depends on poc-user for UserPublicApi)
              ↑
           poc-app      (assembles everything — filters, interceptors, config, migrations)
```

### Clean Architecture Layers

Each module follows this structure:

```
poc-{module}/src/main/java/com/poc/{module}/
├── domain/                    # Entities (innermost layer)
│   └── model/                 # Aggregate root (extends AggregateRoot<String>)
├── contracts/                 # Interfaces (what the app needs)
│   ├── usecases/              # Use case interfaces
│   ├── repositories/          # Repository interfaces
│   └── gateways/              # External system interfaces
├── usecases/                  # Application business rules
│   └── impl/                  # Use case implementations (@Service)
├── interfaceadapters/         # Translates between layers
│   ├── inbound/
│   │   ├── rest/              # REST controllers + DTOs
│   │   └── messaging/         # Kafka consumers
│   ├── outbound/
│   │   ├── persistence/       # Repository adapters
│   │   └── keycloak/          # Keycloak adapter
│   └── presenters/            # Domain → DTO mapping
├── frameworks/                # External integrations (outermost layer)
│   ├── persistence/           # JPA entities + repositories
│   ├── config/                # @Configuration + @ComponentScan
│   └── messaging/             # Kafka configuration
└── publicapi/                 # Public API for other modules
```

### Key Rules
- **Domain objects** have NO JPA annotations
- **Repositories** return domain objects, not JPA entities
- **Only `publicapi/`** is visible to other modules
- **Each module** has its own Postgres schema
- **Events** in `poc-common` enable cross-module communication without compile dependencies
- **Every endpoint** must have `@RequirePermission` or `@RequireRole`
- **Dependencies** point inward: `frameworks/` → `interfaceadapters/` → `usecases/` → `domain/`

## Kong API Gateway

### How it works

Kong runs in **DB-less mode** with a declarative config (`docker/kong/kong.yml`). The config is generated by `scripts/gen-kong-config.sh` which:

1. Waits for Keycloak to be ready
2. Extracts the RSA public key from Keycloak's JWKS endpoint
3. Generates `docker/kong/kong.yml` with:
   - Consumer with JWT secrets (RS256)
   - Routes for each API path
   - CORS plugin per route
   - JWT plugin (validates tokens)
   - Lua post-function (checks permissions, injects X-User-* headers)

### Kong routes

| Route | Path | Required Permissions |
|-------|------|---------------------|
| users | `/api/users` | `user:read` |
| notification-templates | `/api/notification-templates` | `notification:read`, `notification:write` |
| notifications | `/api/notifications` | `notification:read`, `notification:write` |

### JWT → X-User-* headers

The Lua post-function in each route:
1. Decodes the JWT payload
2. Extracts `resource_access.poc-api.roles` (client roles = permissions)
3. Checks if any required permission for the route is present
4. Returns 403 if missing
5. Strips `Authorization` header (app never sees the JWT)
6. Sets headers: `X-User-Id` (sub), `X-User-Roles` (realm roles), `X-User-Permissions` (client roles), `X-User-Name` (preferred_username)

### Hot-reload Kong config

After changing `scripts/gen-kong-config.sh` or adding new routes:
```bash
make kong-reload
```

### Adding a new route

1. Edit `scripts/gen-kong-config.sh`
2. Add a new `make_lua '["permission:read"] = true'` line
3. Add a new route block in the `cat > "$KONG_YML"` section
4. Run `make kong-reload`

## Keycloak Integration

### Auto-setup

The `docker/keycloak/init.sh` script runs automatically on `make infra-up` and creates:
- `poc` realm
- `poc-api` client (with `poc-api-secret`)
- `admin` user (admin/admin)
- Realm roles: `ADMIN`, `USER`
- Client roles (permissions): `user:read`, `user:write`, `notification:read`, `notification:write`
- Composites: `USER` → notification permissions; `ADMIN` → USER + user permissions
- Protocol mapper: realm roles → top-level `roles` claim

### In the user module

- `IdentityProvider` — gateway interface for identity operations
- `KeycloakUserAdapter` — outbound adapter calling Keycloak REST API
- `RegisterUserService` — orchestrates: create Keycloak user → save to DB → publish event
- If DB save fails, rolls back by deleting the Keycloak user

### Get a token

```bash
make token
```

This calls the Keycloak password grant endpoint and returns an access token with:
- `roles` claim (realm roles): ADMIN, USER
- `resource_access.poc-api.roles` claim (client roles): user:read, user:write, notification:read, notification:write

## Vault (Secrets Management)

Vault stores database credentials and Keycloak secrets. The app reads them at startup via Spring Cloud Vault.

### How it works

1. `docker/vault/scripts/start.sh` initializes Vault and unseals it (runs inside container)
2. `scripts/vault-init.sh` enables KV v2, seeds all secrets to `secret/poc`, writes `.vault-token` (runs on host)
3. `Makefile` reads `.vault-token`, exports `VAULT_TOKEN` env var before running the app
4. `application-dev.yml` uses `${VAULT_TOKEN}` to authenticate with Vault

### Token Flow (Dev)

```
make infra-up
  → docker compose up vault (start.sh: init + unseal)
  → vault-init.sh: enable KV v2, seed all secrets (datasource + keycloak), write .vault-token

make run-dev
  → Makefile reads .vault-token → exports VAULT_TOKEN
  → application-dev.yml: token: ${VAULT_TOKEN}
  → vault:// connects with correct token
```

### Configuration

| Profile | Secret Provider | Config Source |
|---------|----------------|---------------|
| `dev` (default for local) | HashiCorp Vault | `application-dev.yml` + Makefile exports token |
| `prod` (default for production) | AWS/GCP/Azure (when configured) | Env vars or `application-prod.yml` |

### Vault Secrets (stored at `secret/poc`)

| Key | Value | Used By |
|-----|-------|---------|
| `spring.datasource.username` | `poc` | HikariCP connection pool |
| `spring.datasource.password` | `poc` | HikariCP connection pool |
| `keycloak.auth-server-url` | `http://keycloak:8080` | `KeycloakProperties` |
| `keycloak.realm` | `poc` | `KeycloakProperties` |
| `keycloak.client-id` | `poc-api` | `KeycloakProperties` |
| `keycloak.client-secret` | `poc-api-secret` | `KeycloakProperties` |
| `keycloak.admin-username` | `admin` | `KeycloakProperties` |
| `keycloak.admin-password` | `admin` | `KeycloakProperties` |

### Local Development

```bash
make run-dev  # Uses dev profile with local Vault
```

The `dev` profile loads:
- `vault://` (Spring Cloud Vault)
- Token provided via `VAULT_TOKEN` env var (set by Makefile)

### Production

```bash
# Option 1: Use env vars
SPRING_DATASOURCE_URL=jdbc:postgresql://prod-host:5432/poc \
SPRING_DATASOURCE_USERNAME=prod-user \
SPRING_DATASOURCE_PASSWORD=prod-pass \
java -jar poc-app.jar

# Option 2: Use application-prod.yml
cp application-prod.yml.example application-prod.yml
# Edit with real values
java -jar poc-app.jar
```

### Adding a new secret provider

1. Add dependency to `poc-app/pom.xml` (e.g., `spring-cloud-starter-aws-secrets-manager-config`)
2. Create `application-{profile}.yml` with provider-specific config
3. Set `spring.profiles.default: {profile}` in `application.yml` for that environment

## Configuration

### Profiles

| Profile | Purpose | Secret Provider |
|---------|---------|-----------------|
| `dev` | Local development | HashiCorp Vault (localhost) |
| `prod` | Production (default) | Env vars or cloud provider |

The default profile is `prod` (set in `application.yml`). Override with `--spring.profiles.active=dev`.

### Config File Order

```
application.yml                    # Base config (no secrets, no datasource defaults)
  └── application-{profile}.yml    # Profile-specific overrides
        └── env vars               # Highest priority, overrides everything
```

### Validation

The app validates required properties at startup using `@ConfigurationProperties` + `@Validated`. If missing, you get a clear error:

```
spring.datasource.url or SPRING_DATASOURCE_URL env var is required
```

### Environment Variables

| Variable | Purpose | Required |
|----------|---------|----------|
| `SPRING_DATASOURCE_URL` | Database URL | Yes (prod) |
| `SPRING_DATASOURCE_USERNAME` | Database username | Yes (prod) |
| `SPRING_DATASOURCE_PASSWORD` | Database password | Yes (prod) |
| `VAULT_TOKEN` | Vault token (dev only) | No (auto-generated) |

### Files

| File | Gitignored | Purpose |
|------|------------|---------|
| `application.yml` | No | Base config (no secrets) |
| `application-dev.yml` | No | Dev profile config (Vault) |
| `application-prod.yml` | Yes | Production config (copy from example) |
| `application-prod.yml.example` | No | Template for production |
| `.vault-token` | Yes | Auto-generated Vault token (by `vault-init.sh`) |

## Flyway Multi-Schema

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

## Database Replication

### Architecture

```
poc-app
  ├── @Transactional (write)           ──→ RoutingDataSource ──→ primary-pool ──→ pgbouncer (:6432) ──→ postgres (master)
  └── @Transactional(readOnly = true)  ──→ RoutingDataSource ──→ replica-pool ──→ pgbouncer-replica (:6433) ──→ postgres-replica

poc-worker (unchanged, master only)    ──→ spring.datasource ──→ pgbouncer (:6432) ──→ postgres (master)
```

### How routing works

The routing is **automatic** via Spring's `@Transactional(readOnly = true)` — no custom annotations or AOP needed.

1. `@Transactional` (default) → `TransactionSynchronizationManager.isCurrentTransactionReadOnly()` returns `false` → routes to **primary** (master)
2. `@Transactional(readOnly = true)` → `isCurrentTransactionReadOnly()` returns `true` → routes to **replica**

`AbstractRoutingDataSource` calls `determineCurrentLookupKey()` before any query executes, so the routing happens at the transaction level — not per-query.

### Which services route where

| Service | Annotation | Routes To |
|---------|-----------|-----------|
| `GetUserService` | `@Transactional(readOnly = true)` | **Replica** |
| `GetNotificationService` | `@Transactional(readOnly = true)` | **Replica** |
| `GetNotificationTemplateService` | `@Transactional(readOnly = true)` | **Replica** |
| `DlqService` (read methods) | `@Transactional(readOnly = true)` | **Replica** |
| `RegisterUserService` | `@Transactional` | **Primary** |
| `SendNotificationService` | `@Transactional` | **Primary** |
| `CreateNotificationTemplateService` | `@Transactional` | **Primary** |
| `DlqService` (write methods) | `@Transactional` | **Primary** |
| `UserRegisteredEmailListener` | `@Transactional` | **Primary** |

**Important:** When a `@Transactional` (write) service calls a `@Transactional(readOnly = true)` service, the inner annotation is ignored — the outer transaction's read-write flag takes precedence. This is correct: within a write transaction, reads must go to master to see uncommitted writes.

### Verify routing is working

Enable debug logging to see which connection pool is used per request:

```yaml
logging:
  level:
    com.zaxxer.hikari: DEBUG
    com.poc.app.config.datasource: DEBUG
```

Then look for log lines like:
```
primary-pool - Connection borrowed from pool
replica-pool - Connection borrowed from pool
```

You can also check which PostgreSQL node has active connections:

```bash
# Master — should show write transactions
docker compose exec postgres psql -U poc -d poc -c \
  "SELECT pid, state, query FROM pg_stat_activity WHERE datname = 'poc';"

# Replica — should show read-only transactions
docker compose exec postgres-replica psql -U poc -d poc -c \
  "SELECT pid, state, query FROM pg_stat_activity WHERE datname = 'poc';"
```

### Risks

| Risk | Severity | Description | Mitigation |
|------|----------|-------------|------------|
| **Replica lag** | Medium | Reads from replica may return slightly stale data (typically < 1s). If a user registers and immediately queries their profile, the replica might not have the data yet. | By design — the outer `@Transactional` in write services naturally reads from master. Document this behavior for API consumers. |
| **Replica goes down** | Medium | If the replica is unreachable, read operations will fail with a connection timeout (default: 30s). | Future enhancement: add a health check filter that falls back to primary if replica is unreachable. For now, restart the replica: `docker compose up -d postgres-replica` |
| **PgBouncer transaction mode** | Low | PgBouncer runs in transaction pooling mode (default). Prepared statements and `SET` commands may not persist across transactions. | Spring Boot's HikariCP and JPA handle this correctly. `SET default_transaction_read_only = on` is sent within each read-only transaction. |
| **Flyway on primary only** | Low | Migrations only run against the primary datasource. The replica gets schema changes via streaming replication. | By design — Flyway runs at startup on the `@Primary` (primary) datasource. The replica receives WAL changes automatically. |
| **Nested transaction routing** | Low | If a write service calls a read service, the read goes to master (not replica). This is correct but may confuse developers expecting all `readOnly` services to hit the replica. | Documented above — outer transaction flag takes precedence. This is by design to ensure consistency within write transactions. |

### Scaling to more replicas

To add additional read replicas:

1. **Docker Compose:** Duplicate `postgres-replica` + `pgbouncer-replica` blocks with unique names/ports:
   ```yaml
   postgres-replica-2:
     image: postgres:16-alpine
     ports:
       - "5434:5432"
     # ... same replica setup ...

   pgbouncer-replica-2:
     ports:
       - "6434:6432"
     depends_on:
       postgres-replica-2:
         condition: service_healthy
   ```

2. **DataSourceConfig:** Add a new `@Bean` for the additional replica and add it to the routing map.

3. **application.yml:** Add the new datasource entry:
   ```yaml
   app:
     datasource:
       replica2:
         hikari:
           jdbc-url: jdbc:postgresql://localhost:6434/poc
   ```

### Cloud migration (AWS RDS / GCP Cloud SQL)

| What stays the same | What changes |
|---------------------|-------------|
| `@Transactional(readOnly = true)` annotations | Replace Docker replica → RDS Read Replica / Cloud SQL replica |
| `RoutingDataSource` pattern | Update `app.datasource.replica.hikari.jdbc-url` → cloud reader endpoint |
| `@Primary` routing logic | Optionally remove PgBouncer (managed services handle pooling) |

**AWS RDS:** Use the reader endpoint (`instance-name.cluster-xxxx.region.rds.amazonaws.com`) as the replica JDBC URL.

**GCP Cloud SQL:** Use the read replica IP address as the replica JDBC URL.

## How to Add a New Module

### Step-by-step

1. **Create directory:**
   ```bash
   mkdir -p poc-{name}/src/main/java/com/poc/{name}/{domain/model,contracts/{usecases,repositories,gateways},usecases,interfaceadapters/{inbound/{rest,messaging},outbound/{persistence,keycloak},presenters},frameworks/{persistence,config,messaging},publicapi}
   ```

2. **Create `poc-{name}/pom.xml`** — parent is `poc-parent`, depends on `poc-common`

3. **Add to parent `pom.xml`** — `<module>poc-{name}</module>`

4. **Add to `poc-app/pom.xml`** — `<dependency>poc-{name}</dependency>`

5. **Create domain model** — extends `AggregateRoot<String>`, has `getId()`, `equals()`, `hashCode()`

6. **Create repository interface** — in `contracts/repositories/`, returns domain objects

7. **Create use case interfaces** — in `contracts/usecases/`

8. **Create use case implementations** — `@Service` in `usecases/`, injects repository interfaces

9. **Create JPA entity** — `@Entity` + `@Table(schema = "poc_{name}")` in `frameworks/persistence/`

10. **Create JPA repository** — extends `JpaRepository` in `frameworks/persistence/`

11. **Create adapter** — `@Component` in `interfaceadapters/outbound/persistence/`, implements repository interface, maps domain ↔ JPA

12. **Create controller** — `@RestController` in `interfaceadapters/inbound/rest/`, `@RequirePermission` on endpoints

13. **Create DTOs** — records in `interfaceadapters/inbound/rest/dto/`

14. **Create presenter** — static mapping methods in `interfaceadapters/presenters/`

15. **Create module config** — `@Configuration` + `@ComponentScan(basePackages = "com.poc.{name}")` in `frameworks/config/`

16. **Create Flyway migration** — `V{N}__create_{name}_tables.sql` in `poc-app/`

17. **Add schema to `application.yml`** — add `poc_{name}` to `spring.flyway.schemas`

18. **Add Kong route** — update `scripts/gen-kong-config.sh` and run `make kong-reload`

### Checklist

- [ ] Domain model extends `AggregateRoot<String>`
- [ ] Repository interface in `contracts/repositories/`
- [ ] Use case interfaces in `contracts/usecases/`
- [ ] Use case implementations in `usecases/`
- [ ] JPA entity in `frameworks/persistence/` with `@Table(schema = "poc_{name}")`
- [ ] Repository adapter in `interfaceadapters/outbound/persistence/`
- [ ] Controller in `interfaceadapters/inbound/rest/` with `@RequirePermission`
- [ ] DTOs in `interfaceadapters/inbound/rest/dto/`
- [ ] Module config in `frameworks/config/` with `@ComponentScan`
- [ ] Flyway migration creates schema + tables
- [ ] Schema in `spring.flyway.schemas`
- [ ] Kong route added in `gen-kong-config.sh`
- [ ] `mvn clean compile -DskipTests` passes

## Makefile Commands

| Command | Description |
|---------|-------------|
| `make help` | Show all available commands |
| `make infra-up` | Start all infrastructure (master + replica, auto-initializes Keycloak + Kong + Vault) |
| `make infra-down` | Stop infrastructure |
| `make infra-reset` | Stop + delete volumes (reset DB) |
| `make build` | Build all modules |
| `make install` | Install all modules (first time) |
| `make run` | Run the application (default profile: prod) |
| `make run-dev` | Run with dev profile (Vault, Swagger) |
| `make test` | Run all tests |
| `make clean` | Clean build artifacts |
| `make status` | Show Docker Compose status |
| `make logs` | Tail infrastructure logs |
| `make logs-kong` | Tail Kong logs |
| `make logs-replica` | Tail PostgreSQL replica logs |
| `make keycloak-setup` | Re-run Keycloak init script |
| `make kong-reload` | Regenerate Kong config from Keycloak JWKS |
| `make token` | Get an access token from Keycloak |
| `make test-api` | Test API via Kong gateway (port 8000) |
| `make test-api-direct` | Test API directly (port 8080, no auth) |
| `make verify-replication` | Verify PostgreSQL streaming replication is working |
| `make psql-master` | Connect to PostgreSQL master via psql |
| `make psql-replica` | Connect to PostgreSQL replica via psql |

## Infrastructure Ports

| Service | Port | Notes |
|---------|------|-------|
| Kong proxy | 8000 | API entry point (JWT validation) |
| Kong admin | 8001 | Admin API (internal) |
| App | 8080 | Spring Boot (direct, no auth) |
| PostgreSQL (master) | 5432 | Direct access |
| PostgreSQL (replica) | 5433 | Direct access (debug) |
| PgBouncer (master) | 6432 | Pooled write access |
| PgBouncer (replica) | 6433 | Pooled read access |
| Keycloak | 8081 | Admin console (admin/admin) |
| Vault | 8200 | Secrets API/UI (root-token) |
| Jaeger | 16686 | Tracing UI |
| Grafana | 3000 | Observability UI (admin/admin) |
| Loki | 3100 | Log storage API |
