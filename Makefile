.PHONY: help infra-up infra-down infra-reset kafka-up build install run run-worker run-worker-email run-dev run-worker-dev start-dev test clean flyway-migrate flyway-info flyway-validate flyway-clean flyway-baseline flyway-repair verify-replication psql-master psql-replica logs-replica

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

infra-up: ## Start infrastructure (PostgreSQL + replica, PgBouncer x2, Keycloak, Kong, Vault, Jaeger, Loki, Grafana)
	docker compose up -d
	@echo "Waiting for services to start..."
	@sleep 15
	@./scripts/vault-init.sh
	@echo "Infrastructure is ready!"

kafka-up: ## Start Kafka + MailHog
	docker compose up -d kafka mailhog kafka-ui
	@echo "Kafka: localhost:9092 | MailHog UI: http://localhost:8025"

start-infra: infra-up kafka-up

infra-down: ## Stop infrastructure
	docker compose down

infra-reset: ## Stop infrastructure and delete volumes (reset DB)
	docker compose down -v
	@echo "Volumes deleted. Run 'make infra-up' to restart."

build: ## Build all modules
	mvn clean compile

install: ## Install all modules (needed first time)
	mvn install -DskipTests

run: ## Run the API server
	mvn -pl poc-app spring-boot:run

run-worker: ## Run the email worker (all handlers)
	mvn -pl poc-worker spring-boot:run

run-worker-email: ## Run the email worker (email handler only)
	mvn -pl poc-worker spring-boot:run -Dspring-boot.run.arguments="--worker.handlers=email"

run-dev: ## Run the API server with dev profile (Swagger enabled)
	VAULT_TOKEN=$$(cat .vault-token 2>/dev/null | sed 's/export VAULT_TOKEN=//') mvn -pl poc-app spring-boot:run -Dspring-boot.run.profiles=dev

run-worker-dev: ## Run the email worker with dev profile
	VAULT_TOKEN=$$(cat .vault-token 2>/dev/null | sed 's/export VAULT_TOKEN=//') mvn -pl poc-worker spring-boot:run -Dspring-boot.run.profiles=dev

start-dev: install run-dev

start-worker-dev: install run-worker-dev

test: ## Run all tests
	mvn test

clean: ## Clean build artifacts
	mvn clean

status: ## Show Docker Compose status
	docker compose ps

logs: ## Tail infrastructure logs
	docker compose logs -f

logs-keycloak: ## Tail Keycloak logs
	docker compose logs -f keycloak

logs-kong: ## Tail Kong logs
	docker compose logs -f kong-dbless

logs-postgres: ## Tail PostgreSQL logs
	docker compose logs -f postgres

logs-replica: ## Tail PostgreSQL replica logs
	docker compose logs -f postgres-replica

verify-replication: ## Verify PostgreSQL streaming replication is working
	@./docker/scripts/verify-replication.sh

psql-master: ## Connect to PostgreSQL master via psql
	docker compose exec postgres psql -U poc -d poc

psql-replica: ## Connect to PostgreSQL replica via psql
	docker compose exec postgres-replica psql -U poc -d poc

logs-kafka: ## Tail Kafka logs
	docker compose logs -f kafka

keycloak-setup: ## Re-run Keycloak init script manually
	docker compose run --rm keycloak-init

kong-reload: ## Regenerate Kong config from Keycloak JWKS and hot-reload
	@./scripts/gen-kong-config.sh
	@curl -s -X POST http://localhost:8001/config \
		-H "Content-Type: application/json" \
		-d @docker/kong/kong.yml > /dev/null 2>&1 && \
		echo "Kong config reloaded via Admin API" || \
		(docker compose restart kong-dbless && echo "Kong restarted (Admin API not available)")

token: ## Get an access token from Keycloak
	@echo "Getting token..."
	@curl -s -X POST http://localhost:8081/realms/poc/protocol/openid-connect/token \
		-H "Content-Type: application/x-www-form-urlencoded" \
		-d "client_id=poc-api" \
		-d "client_secret=poc-api-secret" \
		-d "username=admin" \
		-d "password=admin" \
		-d "grant_type=password" | python3 -m json.tool 2>/dev/null || echo "Keycloak not ready or realm not configured"

test-api: ## Test API via Kong gateway (port 8000)
	@echo "=== Register User ==="
	@curl -s -X POST http://localhost:8000/api/auth/register \
		-H "Content-Type: application/json" \
		-d '{"username":"testuser","email":"test@example.com","password":"test123"}' | python3 -m json.tool 2>/dev/null || echo "Kong or app not running"
	@echo ""
	@echo "=== List Users ==="
	@TOKEN=$$(make -s token 2>/dev/null | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4); \
	curl -s -H "Authorization: Bearer $$TOKEN" http://localhost:8000/api/users | python3 -m json.tool 2>/dev/null || echo "Kong or app not running, or token expired"

test-api-direct: ## Test API directly (port 8080, no auth)
	@echo "=== Register User (direct) ==="
	@curl -s -X POST http://localhost:8080/api/auth/register \
		-H "Content-Type: application/json" \
		-d '{"username":"testuser","email":"test@example.com","password":"test123"}' | python3 -m json.tool 2>/dev/null || echo "App not running"
	@echo ""
	@echo "=== List Users (direct) ==="
	@curl -s http://localhost:8080/api/users | python3 -m json.tool 2>/dev/null || echo "App not running"

sleep:
	sleep 15

DB_URL ?= jdbc:postgresql://localhost:5432/poc
DB_USER ?= poc
DB_PASS ?= poc
DB_SCHEMAS ?= poc_user

flyway-migrate: ## Run Flyway migrations
	mvn -pl poc-app flyway:migrate \
		-Dflyway.url=$(DB_URL) \
		-Dflyway.user=$(DB_USER) \
		-Dflyway.password=$(DB_PASS) \
		-Dflyway.schemas=$(DB_SCHEMAS)

flyway-info: ## Show Flyway migration status
	mvn -pl poc-app flyway:info \
		-Dflyway.url=$(DB_URL) \
		-Dflyway.user=$(DB_USER) \
		-Dflyway.password=$(DB_PASS) \
		-Dflyway.schemas=$(DB_SCHEMAS)

flyway-validate: ## Validate Flyway migrations
	mvn -pl poc-app flyway:validate \
		-Dflyway.url=$(DB_URL) \
		-Dflyway.user=$(DB_USER) \
		-Dflyway.password=$(DB_PASS) \
		-Dflyway.schemas=$(DB_SCHEMAS)

flyway-clean: ## Clean database (DEVELOPMENT ONLY - deletes all data!)
	@echo "WARNING: This will delete all data in the database!"
	@read -p "Are you sure? (y/N): " confirm; \
	if [ "$$confirm" = "y" ] || [ "$$confirm" = "Y" ]; then \
		mvn -pl poc-app flyway:clean \
			-Dflyway.url=$(DB_URL) \
			-Dflyway.user=$(DB_USER) \
			-Dflyway.password=$(DB_PASS) \
			-Dflyway.schemas=$(DB_SCHEMAS); \
	else \
		echo "Cancelled."; \
	fi

flyway-repair: ## Repair Flyway schema history (fix checksum mismatches)
	mvn -pl poc-app flyway:repair \
		-Dflyway.url=$(DB_URL) \
		-Dflyway.user=$(DB_USER) \
		-Dflyway.password=$(DB_PASS) \
		-Dflyway.schemas=$(DB_SCHEMAS)

flyway-baseline: ## Baseline existing database (for first-time setup)
	mvn -pl poc-app flyway:baseline \
		-Dflyway.url=$(DB_URL) \
		-Dflyway.user=$(DB_USER) \
		-Dflyway.password=$(DB_PASS) \
		-Dflyway.schemas=$(DB_SCHEMAS)