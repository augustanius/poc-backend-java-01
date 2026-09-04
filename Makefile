.PHONY: help infra-up infra-down infra-reset build run test clean

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

infra-up: ## Start infrastructure (PostgreSQL, PgBouncer, Keycloak, Vault, Jaeger, Loki, Grafana)
	docker compose up -d
	@echo "Waiting for services to start..."
	@sleep 15
	@./scripts/vault-init.sh
	@echo "Infrastructure is ready!"

infra-down: ## Stop infrastructure
	docker compose down

infra-reset: ## Stop infrastructure, delete volumes, and reinitialize everything
	docker compose down -v
	@echo "Volumes deleted. Starting infrastructure..."
	@./scripts/vault-init.sh
	@echo "Infrastructure is ready!"

build: ## Build all modules
	mvn clean compile

install: ## Install all modules (needed first time)
	mvn install -DskipTests

run: ## Run the application
	@if [ -f .vault-token ]; then \
		export $$(cat .vault-token | xargs); \
	fi && \
	mvn -pl poc-app spring-boot:run

run-dev: ## Run the application with dev profile (Swagger enabled)
	@if [ -f .vault-token ]; then \
		export $$(cat .vault-token | xargs); \
	fi && \
	mvn -pl poc-app spring-boot:run -Dspring-boot.run.profiles=dev

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

logs-postgres: ## Tail PostgreSQL logs
	docker compose logs -f postgres

vault-init: ## Initialize Vault with secrets
	@./scripts/vault-init.sh

vault-token: ## Show current Vault root token
	@if [ -f .vault-token ]; then \
		cat .vault-token; \
	else \
		echo "No token found. Run 'make vault-init' first."; \
	fi

keycloak-setup: ## Import Keycloak realm (manual step - see README.md)
	@echo "Open Keycloak Admin Console at http://localhost:8081"
	@echo "Login with admin/admin"
	@echo "Create realm 'poc', client 'poc-api', and roles as described in README.md"

token: ## Get an access token from Keycloak
	@echo "Getting token..."
	@curl -s -X POST http://localhost:8081/realms/poc/protocol/openid-connect/token \
		-H "Content-Type: application/x-www-form-urlencoded" \
		-d "client_id=poc-api" \
		-d "client_secret=poc-api-secret" \
		-d "username=admin" \
		-d "password=admin" \
		-d "grant_type=password" | python3 -m json.tool 2>/dev/null || echo "Keycloak not ready or realm not configured"

test-api: ## Test API via curl (requires token)
	@echo "=== List Users ==="
	@curl -s -H "Authorization: Bearer $$(make -s token 2>/dev/null | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)" \
		http://localhost:8080/api/users | python3 -m json.tool 2>/dev/null || echo "App not running or token expired"
