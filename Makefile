.PHONY: help infra-up infra-down infra-reset build run test clean

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

infra-up: ## Start infrastructure (PostgreSQL, PgBouncer, Keycloak, Kong, Vault, Jaeger, Loki, Grafana)
	docker compose up -d
	@echo "Waiting for services to start..."
	@sleep 15
	@./scripts/vault-init.sh
	@echo "Infrastructure is ready!"

infra-down: ## Stop infrastructure
	docker compose down

infra-reset: ## Stop infrastructure and delete volumes (reset DB)
	docker compose down -v
	@echo "Volumes deleted. Run 'make infra-up' to restart."

build: ## Build all modules
	mvn clean compile

install: ## Install all modules (needed first time)
	mvn install -DskipTests

run: ## Run the application
	mvn -pl poc-app spring-boot:run

run-dev: ## Run the application with dev profile (Swagger enabled)
	VAULT_TOKEN=$$(cat .vault-token 2>/dev/null | sed 's/export VAULT_TOKEN=//') mvn -pl poc-app spring-boot:run -Dspring-boot.run.profiles=dev

start-dev: install run-dev

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
	@echo "=== List Users ==="
	@TOKEN=$$(make -s token 2>/dev/null | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4); \
	curl -s -H "Authorization: Bearer $$TOKEN" http://localhost:8000/api/users | python3 -m json.tool 2>/dev/null || echo "Kong or app not running, or token expired"

test-api-direct: ## Test API directly (port 8080, no auth)
	@echo "=== List Users (direct, no auth) ==="
	@curl -s http://localhost:8080/api/users | python3 -m json.tool 2>/dev/null || echo "App not running"
