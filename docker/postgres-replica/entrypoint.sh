#!/bin/bash
set -e

REPLICA_PRIMARY_HOST=${REPLICA_PRIMARY_HOST:-postgres}
REPLICA_PRIMARY_PORT=${REPLICA_PRIMARY_PORT:-5432}
REPLICA_USER=${REPLICA_USER:-replicator}
REPLICA_PASSWORD=${REPLICA_PASSWORD:-replicator}
DATA_DIR="/var/lib/postgresql/data"

if [ -z "$(ls -A "$DATA_DIR" 2>/dev/null)" ]; then
    echo "Running pg_basebackup from ${REPLICA_PRIMARY_HOST}:${REPLICA_PRIMARY_PORT}..."
    PGPASSWORD="$REPLICA_PASSWORD" pg_basebackup \
        -h "$REPLICA_PRIMARY_HOST" \
        -p "$REPLICA_PRIMARY_PORT" \
        -U "$REPLICA_USER" \
        -D "$DATA_DIR" \
        -Fp -Xs -P -R

    echo "Base backup complete. Starting as standby."
fi

touch "$DATA_DIR/standby.signal"

cat >> "$DATA_DIR/postgresql.conf" <<EOF
hot_standby = on
max_wal_senders = 5
wal_receiver_timeout = 60s
EOF

exec docker-entrypoint.sh postgres
