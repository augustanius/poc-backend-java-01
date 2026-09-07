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

# Enable KV v2 secrets engine (idempotent)
echo "Enabling KV v2 secrets engine..."
curl -sf \
    -H "X-Vault-Token: ${VAULT_TOKEN}" \
    -X POST \
    -d '{"type":"kv","options":{"version":"2"}}' \
    "${VAULT_ADDR}/v1/sys/mounts/secret" > /dev/null 2>&1 || true
echo "KV v2 engine ready."

echo "Seeding Vault secrets..."
curl -sf \
    -H "X-Vault-Token: ${VAULT_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{"data":{"spring.datasource.username":"poc","spring.datasource.password":"poc"}}' \
    "${VAULT_ADDR}/v1/secret/data/poc/dev" > /dev/null

echo "Vault secrets seeded successfully!"
echo "  - spring.datasource.username = poc"
echo "  - spring.datasource.password = poc"

# Export token for shell usage (e.g., make run)
echo ""
echo "export VAULT_TOKEN=${VAULT_TOKEN}" > .vault-token
echo "Token saved to .vault-token"
echo "Run 'source .vault-token' or use 'make run' to connect."
