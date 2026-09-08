# PoC Modular Monolith - Developer Guide

## Overview

This is a modular monolith built with Spring Boot 4, demonstrating hexagonal (ports & adapters) architecture across two business modules: **User** and **Notification**, with a separate **Worker** for async event processing via Kafka. Infrastructure includes Kong API Gateway, PostgreSQL, PgBouncer, Kafka (KRaft), MailHog, OpenTelemetry (Jaeger + Loki + Grafana), HashiCorp Vault, and Keycloak.

## Module Dependency Graph

```
poc-common       (shared kernel, base classes, domain events, outbox)
    ↑ compile
    ├── poc-user      (User module + Keycloak integration)
    └── poc-notification (Notification module, compile-depends on poc-user)
              ↑ runtime-only
           poc-app     (assembles everything, @SpringBootApplication)
           poc-worker  (async event processor, @SpringBootApplication)
```

### Rules

- `poc-common` has NO dependencies on other modules
- `poc-user` depends ONLY on `poc-common`
- `poc-notification` depends on `poc-common` AND `poc-user` (for `UserPublicApi`)
- `poc-app` depends on ALL modules (assembles everything)
- every commit need using conventional commit

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

| File                       | Purpose                                                         |
| -------------------------- | --------------------------------------------------------------- |
| `AggregateRoot.java`       | Base class for domain models — enforces `getId()` contract      |
| `DomainEvent.java`         | Base class for events — auto-generates `eventId` + `occurredAt` |
| `PageResult.java`          | Generic pagination record with `map()` for DTO conversion       |
| `UserContext.java`         | ThreadLocal holder for X-User-\* headers from gateway           |
| `RequirePermission.java`   | Annotation for permission-based access control                  |
| `RequireRole.java`         | Annotation for role-based access control                        |
| `AuthenticatedUserId.java` | Parameter annotation resolved by argument resolver              |
| `UserRegisteredEvent.java` | Event published when a user registers                           |

### poc-user

| File                         | Purpose                                                           |
| ---------------------------- | ----------------------------------------------------------------- |
| `User.java`                  | Domain model — extends `AggregateRoot<String>`                    |
| `UserRepository.java`        | SPI — outbound port for persistence                               |
| `IdentityProviderPort.java`  | SPI — port for Keycloak integration                               |
| `GetUserUseCase.java`        | Use case — get/search users                                       |
| `RegisterUserUseCase.java`   | Use case — register (Keycloak + DB + event)                       |
| `GetUserService.java`        | Implementation — delegates to repository                          |
| `RegisterUserService.java`   | Implementation — orchestrates identity + DB + event with rollback |
| `AuthController.java`        | REST controller — `POST /api/auth/register` (public)              |
| `AuthDto.java`               | Request/response records for auth                                  |
| `UserController.java`        | REST controller — `@RequirePermission`, read-only                 |
| `UserDto.java`               | Request/response records                                          |
| `UserJpaEntity.java`         | JPA entity — `@Table(schema = "poc_user")`                        |
| `UserJpaRepository.java`     | Spring Data repository                                            |
| `UserRepositoryAdapter.java` | Adapter — implements SPI, maps domain ↔ JPA                       |
| `KeycloakUserAdapter.java`   | Adapter — implements IdentityProviderPort via REST API            |
| `KeycloakProperties.java`    | `@ConfigurationProperties` for Keycloak config                    |
| `UserModuleConfig.java`      | `@Configuration` — wires beans, `@ComponentScan`                  |
| `UserPublicApi.java`         | Public API — interface for other modules                          |

### poc-notification

| File                                         | Purpose                                                           |
| -------------------------------------------- | ----------------------------------------------------------------- |
| `Notification.java`                          | Aggregate Root #1 — extends `AggregateRoot<String>`               |
| `NotificationTemplate.java`                  | Aggregate Root #2 — extends `AggregateRoot<String>`               |
| `NotificationRepository.java`                | SPI — outbound port for Notification                              |
| `NotificationTemplateRepository.java`        | SPI — outbound port for NotificationTemplate                      |
| `SendNotificationUseCase.java`               | Use case — send notification (calls `UserPublicApi`)              |
| `GetNotificationUseCase.java`                | Use case — get/search notifications                               |
| `CreateNotificationTemplateUseCase.java`     | Use case — create template                                        |
| `GetNotificationTemplateUseCase.java`        | Use case — get/search templates                                   |
| `SendNotificationService.java`               | Implementation — cross-module: validates user via `UserPublicApi` |
| `GetNotificationService.java`                | Implementation — delegates to repository                          |
| `CreateNotificationTemplateService.java`     | Implementation — delegates to repository                          |
| `GetNotificationTemplateService.java`        | Implementation — delegates to repository                          |
| `NotificationController.java`                | REST controller — `@RequirePermission`, ownership checks          |
| `NotificationDto.java`                       | Request/response records                                          |
| `NotificationTemplateController.java`        | REST controller — admin endpoints                                 |
| `NotificationTemplateDto.java`               | Request/response records                                          |
| `NotificationJpaEntity.java`                 | JPA entity — `@Table(schema = "poc_notification")`                |
| `NotificationJpaRepository.java`             | Spring Data repository                                            |
| `NotificationRepositoryAdapter.java`         | Adapter — implements SPI, maps domain ↔ JPA                       |
| `NotificationTemplateJpaEntity.java`         | JPA entity — `@Table(schema = "poc_notification")`                |
| `NotificationTemplateJpaRepository.java`     | Spring Data repository                                            |
| `NotificationTemplateRepositoryAdapter.java` | Adapter — implements SPI, maps domain ↔ JPA                       |
| `NotificationModuleConfig.java`              | `@Configuration` — `@ComponentScan`                               |
| `NotificationPublicApi.java`                 | Public API — interface for other modules                          |

### poc-app

| File                                       | Purpose                                                 |
| ------------------------------------------ | ------------------------------------------------------- |
| `PocApplication.java`                      | `@SpringBootApplication` entry point                    |
| `UserContextFilter.java`                   | Reads X-User-\* headers → ThreadLocal                   |
| `WebMvcConfig.java`                        | Registers interceptor + argument resolver + filter      |
| `PermissionInterceptor.java`               | Enforces `@RequirePermission` / `@RequireRole`          |
| `AuthenticatedUserIdArgumentResolver.java` | Resolves `@AuthenticatedUserId` parameter               |
| `application.yml`                          | Vault, Keycloak, multi-schema Flyway, datasource config |
| `V1__create_users_table.sql`               | Flyway migration — `poc_user` schema                    |
| `V2__create_notifications_table.sql`       | Flyway migration — `poc_notification` schema            |
| `V3__create_outbox_events_table.sql`       | Flyway migration — `poc_outbox` schema                  |
| `UserRegisteredEmailListener.java`         | Listens for UserRegisteredEvent → queues welcome email  |

### poc-worker

| File                         | Purpose                                                           |
| ---------------------------- | ----------------------------------------------------------------- |
| `WorkerApplication.java`     | `@SpringBootApplication` entry point (no web)                     |
| `KafkaConfig.java`           | Producer/consumer beans, `@EnableKafka`                           |
| `EventHandler.java`          | Interface for event handlers                                      |
| `EventDispatcher.java`       | Routes events by type, filters by `--worker.handlers` CLI arg     |
| `EmailEventHandler.java`     | Sends email via Spring JavaMailSender                              |
| `OutboxPoller.java`          | `@Scheduled` poller — claims outbox events → Kafka                 |
| `EmailConsumer.java`         | `@KafkaListener` — receives from Kafka → dispatches to handler    |
| `application.yml`            | Kafka, mail (MailHog), outbox, worker config                      |

## Kafka & Event-Driven Architecture

### Overview

The app uses the **Transactional Outbox Pattern** for reliable event delivery to Kafka. Events are written to the `outbox_events` table in the **same transaction** as business data, then published to Kafka by a background poller.

### Flow

```
┌─────────────────────────────────────────────────────────────────┐
│  poc-app (API Server)                                           │
│                                                                 │
│  RegisterUserService                                            │
│    ├── DB: save user                                            │
│    └── ApplicationEventPublisher → UserRegisteredEvent          │
│                                                                 │
│  UserRegisteredEmailListener (@EventListener)                   │
│    ├── Creates Notification (PENDING) in DB                     │
│    └── OutboxPublisher → outbox_event (same @Transactional)     │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌───────────────────────────────────────────────────┐
│  PostgreSQL: poc_outbox.outbox_events             │
│  (SELECT FOR UPDATE SKIP LOCKED)                  │
└───────────────────┬───────────────────────────────┘
                    │
┌───────────────────┴───────────────────────────────┐
│  poc-worker (Email Worker)                        │
│                                                   │
│  OutboxPoller (@Scheduled 500ms)                  │
│    ├── Claims batch of unpublished events         │
│    └── KafkaTemplate.send() → Kafka               │
│                                                   │
│  EmailConsumer (@KafkaListener)                   │
│    ├── Routes by eventType via EventDispatcher    │
│    ├── EmailEventHandler → Spring JavaMailSender  │
│    └── Updates Notification → SENT or FAILED      │
└───────────────────┬───────────────────────────────┘
                    │
┌───────────────────┴───────────────────────────────┐
│  Kafka (KRaft 3.9.1)                              │
│  Topic: poc.dev.email.events.v1                   │
└───────────────────┬───────────────────────────────┘
                    │
┌───────────────────┴───────────────────────────────┐
│  MailHog (Docker)                                 │
│  SMTP: :1025  |  Web UI: :8025                    │
└───────────────────────────────────────────────────┘
```

### Kafka Topics

| Topic | Purpose |
|-------|---------|
| `poc.dev.email.events.v1` | Email notification events |
| `poc.dev.email.dlq.v1` | Dead letter queue for failed emails |

### Running the Worker

```bash
# Start infrastructure
docker compose up -d

# API server (default)
java -jar poc-app/target/poc-app-0.0.1-SNAPSHOT.jar

# Email worker (all handlers)
java -jar poc-worker/target/poc-worker-0.0.1-SNAPSHOT.jar

# Email worker (only email handler)
java -jar poc-worker/target/poc-worker-0.0.1-SNAPSHOT.jar --worker.handlers=email

# Scale email workers
java -jar poc-worker/target/poc-worker-0.0.1-SNAPSHOT.jar --worker.handlers=email &
java -jar poc-worker/target/poc-worker-0.0.1-SNAPSHOT.jar --worker.handlers=email &
```

### Adding a New Event Handler

1. Create a class implementing `EventHandler` in `poc-worker/handler/`:

```java
@Component
public class SmsEventHandler implements EventHandler {
    @Override
    public String name() { return "sms"; }

    @Override
    public String eventType() { return "SmsNotificationRequested"; }

    @Override
    public void handle(OutboxEvent event) {
        // call Twilio, Vonage, etc.
    }
}
```

2. Enable it: `--worker.handlers=email,sms`

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
          - poc_{name} # ADD THIS
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
