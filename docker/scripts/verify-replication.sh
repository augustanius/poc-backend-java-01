#!/bin/bash
# Verify streaming replication is working

POSTGRES_CONTAINER=${1:-poc-java-boilerplate-postgres-1}
REPLICA_CONTAINER=${2:-poc-java-boilerplate-postgres-replica-1}

echo "=== Master: checking replication status ==="
docker exec "$POSTGRES_CONTAINER" psql -U poc -d poc -c \
  "SELECT client_addr, state, sent_lsn, write_lsn, replay_lsn FROM pg_stat_replication;"

echo ""
echo "=== Replica: checking recovery status ==="
docker exec "$REPLICA_CONTAINER" psql -U poc -d poc -c \
  "SELECT pg_is_in_recovery();"

echo ""
echo "=== Replica: verifying data ==="
docker exec "$REPLICA_CONTAINER" psql -U poc -d poc -c \
  "SELECT schemaname, tablename FROM pg_tables WHERE schemaname LIKE 'poc_%';"
