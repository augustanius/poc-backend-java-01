# PoC Modular Monolith - Developer Guide

## Overview

This is a modular monolith built with Spring Boot 4, demonstrating hexagonal (ports & adapters) architecture across two business modules: **User** and **Notification**. Infrastructure includes PostgreSQL, PgBouncer, OpenTelemetry (Jaeger + Loki + Grafana), HashiCorp Vault, and Keycloak.

## Module Dependency Graph

```
poc-common       (shared kernel, base classes, domain events)
    ↑ compile
    ├── poc-user      (User module + Keycloak integration)
    └── poc-notification (Notification module, compile-depends on poc-user)
              ↑ runtime-only
           poc-app     (assembles everything, @SpringBootApplication)
```

### Rules
- `poc-common` has NO dependencies on other modules
- `poc-user` depends ONLY on `poc-common`
- `poc-notification` depends on `poc-common` AND `poc-user` (for `UserPublicApi`)
- `poc-app` depends on ALL modules (assembles everything)

## Hexagonal Architecture Per Module

Each module follows this structure:

```
poc-{module}/src/main/java/com/poc/{module}/
├── domain/
│   ├── model/          # Aggregate root, value objects, domain events
│   └── spi/            # Service Provider Interfaces (outbound ports)
├── application/
│   ├── api/            # Inbound ports (use case interfaces)
│   └── service/        # Use case implementations
├── infrastructure/
│   ├── inbound/        # Driving adapters (REST controllers, DTOs)
│   ├── outbound/       # Driven adapters (JPA entities, repos, adapters)
│   └── config/         # Spring @Configuration for this module
└── publicapi/          # Public API for other modules
```

## Inter-Module Communication

### Pattern 1: Direct Public API
- `poc-user` exposes `UserPublicApi` interface via `publicapi/` package
- `poc-notification` depends on `poc-user` (compile-time) and calls `UserPublicApi`
- Only `publicapi/` classes are visible; internal `domain/` and `infrastructure/` are NOT exported

### Pattern 2: Event-Driven
- `poc-common` defines shared event POJOs (e.g., `UserRegisteredEvent`)
- Any module can publish via Spring `ApplicationEventPublisher`
- Any module can subscribe via `@EventListener` — NO compile dependency needed

## Key Files Reference

### poc-common

| File | Purpose |
|---|---|
| `AggregateRoot.java` | Base class for domain models — enforces `getId()` contract |
| `DomainEvent.java` | Base class for events — auto-generates `eventId` + `occurredAt` |
| `PageResult.java` | Generic pagination record with `map()` for DTO conversion |
| `UserContext.java` | ThreadLocal holder for X-User-* headers from gateway |
| `RequirePermission.java` | Annotation for permission-based access control |
| `RequireRole.java` | Annotation for role-based access control |
| `AuthenticatedUserId.java` | Parameter annotation resolved by argument resolver |
| `UserRegisteredEvent.java` | Event published when a user registers |

### poc-user

| File | Purpose |
|---|---|
| `User.java` | Domain model — extends `AggregateRoot<String>` |
| `UserRepository.java` | SPI — outbound port for persistence |
| `IdentityProviderPort.java` | SPI — port for Keycloak integration |
| `CreateUserUseCase.java` | Use case — create a user |
| `GetUserUseCase.java` | Use case — get/search users |
| `RegisterUserUseCase.java` | Use case — register (Keycloak + DB + event) |
| `CreateUserService.java` | Implementation — delegates to repository |
| `GetUserService.java` | Implementation — delegates to repository |
| `RegisterUserService.java` | Implementation — orchestrates identity + DB + event with rollback |
| `UserController.java` | REST controller — `@RequirePermission`, ownership checks |
| `UserDto.java` | Request/response records |
| `UserJpaEntity.java` | JPA entity — `@Table(schema = "poc_user")` |
| `UserJpaRepository.java` | Spring Data repository |
| `UserRepositoryAdapter.java` | Adapter — implements SPI, maps domain ↔ JPA |
| `KeycloakUserAdapter.java` | Adapter — implements IdentityProviderPort via REST API |
| `KeycloakProperties.java` | `@ConfigurationProperties` for Keycloak config |
| `UserModuleConfig.java` | `@Configuration` — wires beans, `@ComponentScan` |
| `UserPublicApi.java` | Public API — interface for other modules |

### poc-notification

| File | Purpose |
|---|---|
| `Notification.java` | Aggregate Root #1 — extends `AggregateRoot<String>` |
| `NotificationTemplate.java` | Aggregate Root #2 — extends `AggregateRoot<String>` |
| `NotificationRepository.java` | SPI — outbound port for Notification |
| `NotificationTemplateRepository.java` | SPI — outbound port for NotificationTemplate |
| `SendNotificationUseCase.java` | Use case — send notification (calls `UserPublicApi`) |
| `GetNotificationUseCase.java` | Use case — get/search notifications |
| `CreateNotificationTemplateUseCase.java` | Use case — create template |
| `GetNotificationTemplateUseCase.java` | Use case — get/search templates |
| `SendNotificationService.java` | Implementation — cross-module: validates user via `UserPublicApi` |
| `GetNotificationService.java` | Implementation — delegates to repository |
| `CreateNotificationTemplateService.java` | Implementation — delegates to repository |
| `GetNotificationTemplateService.java` | Implementation — delegates to repository |
| `NotificationController.java` | REST controller — `@RequirePermission`, ownership checks |
| `NotificationDto.java` | Request/response records |
| `NotificationTemplateController.java` | REST controller — admin endpoints |
| `NotificationTemplateDto.java` | Request/response records |
| `NotificationJpaEntity.java` | JPA entity — `@Table(schema = "poc_notification")` |
| `NotificationJpaRepository.java` | Spring Data repository |
| `NotificationRepositoryAdapter.java` | Adapter — implements SPI, maps domain ↔ JPA |
| `NotificationTemplateJpaEntity.java` | JPA entity — `@Table(schema = "poc_notification")` |
| `NotificationTemplateJpaRepository.java` | Spring Data repository |
| `NotificationTemplateRepositoryAdapter.java` | Adapter — implements SPI, maps domain ↔ JPA |
| `NotificationModuleConfig.java` | `@Configuration` — `@ComponentScan` |
| `NotificationPublicApi.java` | Public API — interface for other modules |

### poc-app

| File | Purpose |
|---|---|
| `PocApplication.java` | `@SpringBootApplication` entry point |
| `UserContextFilter.java` | Reads X-User-* headers → ThreadLocal |
| `WebMvcConfig.java` | Registers interceptor + argument resolver + filter |
| `PermissionInterceptor.java` | Enforces `@RequirePermission` / `@RequireRole` |
| `AuthenticatedUserIdArgumentResolver.java` | Resolves `@AuthenticatedUserId` parameter |
| `application.yml` | Vault, Keycloak, multi-schema Flyway, datasource config |
| `V1__create_users_table.sql` | Flyway migration — `poc_user` schema |
| `V2__create_notifications_table.sql` | Flyway migration — `poc_notification` schema |

## How to Add a New Module

### Step-by-step

1. **Create module directory:**
   ```bash
   mkdir -p poc-{name}/src/main/java/com/poc/{name}/{domain/{model,spi},application/{api,service},infrastructure/{inbound/web,outbound/persistence,config},publicapi}
   ```

2. **Create `poc-{name}/pom.xml`:**
   ```xml
   <parent>
       <groupId>com.poc</groupId>
       <artifactId>poc-parent</artifactId>
       <version>0.0.1-SNAPSHOT</version>
   </parent>
   <artifactId>poc-{name}</artifactId>
   <dependencies>
       <dependency>
           <groupId>com.poc</groupId>
           <artifactId>poc-common</artifactId>
       </dependency>
       <!-- add spring-boot-starter-web, spring-boot-starter-data-jpa, postgresql -->
   </dependencies>
   ```

3. **Add module to parent `pom.xml`:**
   ```xml
   <modules>
       <!-- existing modules -->
       <module>poc-{name}</module>
   </modules>
   ```

4. **Add module to `poc-app/pom.xml`:**
   ```xml
   <dependencies>
       <!-- existing modules -->
       <dependency>
           <groupId>com.poc</groupId>
           <artifactId>poc-{name}</artifactId>
       </dependency>
   </dependencies>
   ```

5. **Create domain model** (extends `AggregateRoot<String>`) in `domain/model/`

6. **Create SPI** (outbound port interface) in `domain/spi/`

7. **Create use cases** (inbound port interfaces) in `application/api/`

8. **Create service implementations** in `application/service/`

9. **Create JPA entity** (`@Entity` + `@Table(schema = "poc_{name}")`) in `infrastructure/outbound/persistence/`

10. **Create JPA repository** (extends `JpaRepository`) in `infrastructure/outbound/persistence/`

11. **Create adapter** (`@Component`, implements SPI, maps domain ↔ JPA) in `infrastructure/outbound/persistence/`

12. **Create controller** (`@RestController`, `@RequirePermission`) in `infrastructure/inbound/web/`

13. **Create DTO** (request/response records) in `infrastructure/inbound/web/`

14. **Create module config** (`@Configuration` + `@ComponentScan`) in `infrastructure/config/`

15. **Create Flyway migration** (`V{N}__create_{name}_tables.sql`) in `poc-app/src/main/resources/db/migration/`

16. **Add schema to `application.yml`:**
    ```yaml
    spring:
      flyway:
        schemas:
          - poc_user
          - poc_notification
          - poc_{name}  # ADD THIS
    ```

### Checklist before done

- [ ] Domain model extends `AggregateRoot<String>` with `getId()`, `equals()`, `hashCode()`
- [ ] SPI returns domain objects, not JPA entities
- [ ] Service is `@Service` with constructor injection
- [ ] JPA entity has `@Table(schema = "poc_{name}")`
- [ ] Controller has `@RequirePermission` on every endpoint
- [ ] Module config has `@ComponentScan(basePackages = "com.poc.{name}")`
- [ ] Flyway migration creates schema + tables
- [ ] Schema added to `spring.flyway.schemas` in `application.yml`
- [ ] `mvn clean compile -DskipTests` passes

## Conventions

- **Domain objects** in `domain/model/` — NO JPA annotations
- **SPIs** in `domain/spi/` — other modules never import `infrastructure/`
- **Public APIs** in `publicapi/` — only cross-module interface
- **Events** in `poc-common/event/` — shared event POJOs
- **Each module** has its own Postgres schema (`poc_{name}`)
- **DTOs** are Java records, not JPA entities in controllers
- **Ownership checks** via `UserContext.hasRole("ADMIN")` pattern
- **No `open-in-view: true`** — explicit fetches only
