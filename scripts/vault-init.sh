#!/bin/bash
set -e

VAULT_ADDR="${VAULT_ADDR:-http://localhost:8200}"
MAX_RETRIES=30
RETRY_INTERVAL=2

echo "Waiting for Vault to be ready..."
for i in $(seq 1 $MAX_RETRIES); do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${VAULT_ADDR}/v1/sys/health" 2>/dev/null || true)
    if [ "$HTTP_CODE" = "200" ]; then
        echo "Vault is ready!"
        break
    fi
    if [ "$i" -eq "$MAX_RETRIES" ]; then
        echo "ERROR: Vault did not become ready after ${MAX_RETRIES} attempts"
        exit 1
    fi
    echo "  Attempt $i/$MAX_RETRIES - Vault not ready yet, retrying in ${RETRY_INTERVAL}s..."
    sleep $RETRY_INTERVAL
done

# Read root token from Vault container
echo "Reading root token from Vault..."
VAULT_TOKEN=$(docker compose exec -T vault cat /vault/file/init.json 2>/dev/null | python3 -c "import sys,json; print(json.load(sys.stdin)['root_token'])")
if [ -z "$VAULT_TOKEN" ]; then
    echo "ERROR: Could not read root token from Vault. Is Vault initialized?"
    exit 1
fi
echo "Root token loaded."

VAULT_EXEC="docker compose exec -T -e VAULT_TOKEN=${VAULT_TOKEN} -e VAULT_ADDR=http://127.0.0.1:8200 vault"

# Enable KV v2 secrets engine (idempotent)
echo "Enabling KV v2 secrets engine..."
${VAULT_EXEC} vault secrets enable -path=secret kv-v2 2>/dev/null || true
echo "KV v2 engine ready."

# Seed all secrets
echo "Seeding Vault secrets..."
${VAULT_EXEC} vault kv put secret/poc \
    spring.datasource.username=poc \
    spring.datasource.password=poc \
    keycloak.auth-server-url=http://localhost:8081 \
    keycloak.realm=poc \
    keycloak.client-id=poc-api \
    keycloak.client-secret=poc-api-secret \
    keycloak.admin-username=admin \
    keycloak.admin-password=admin
echo "Vault secrets seeded successfully!"

# Export token for shell usage (e.g., make run)
echo ""
echo "export VAULT_TOKEN=${VAULT_TOKEN}" > .vault-token
echo "Token saved to .vault-token"
echo "Run 'source .vault-token' or use 'make run' to connect."
