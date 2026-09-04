#!/bin/sh

INIT_FILE="/vault/file/init.json"
CONFIG_FILE="/vault/config/vault.hcl"
export VAULT_ADDR="http://127.0.0.1:8200"

# Start Vault server in background
vault server -config="$CONFIG_FILE" &
VAULT_PID=$!

# Wait for Vault to be ready
echo "Waiting for Vault to start..."
for i in $(seq 1 30); do
  if nc -z 127.0.0.1 8200 2>/dev/null; then
    echo "Vault is ready."
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo "ERROR: Vault did not start in time"
    exit 1
  fi
  sleep 1
done

# First run: initialize Vault
if [ ! -f "$INIT_FILE" ]; then
  echo "Initializing Vault..."
  vault operator init \
    -key-shares=1 \
    -key-threshold=1 \
    -format=json > "$INIT_FILE"
  echo "Vault initialized."
fi

# Read unseal key (flatten JSON to single line, collapse whitespace)
UNSEAL_KEY=$(cat "$INIT_FILE" | tr '\n' ' ' | tr -s ' ' | sed 's/.*"unseal_keys_b64": *\[ *"//;s/".*//')

# Unseal Vault using vault CLI
echo "Unsealing Vault..."
vault operator unseal "$UNSEAL_KEY"
echo "Vault unsealed."

# Keep container running by waiting for the server process
wait $VAULT_PID
