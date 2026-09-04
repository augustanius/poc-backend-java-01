#!/bin/bash
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SET password_encryption = 'md5';
    ALTER USER $POSTGRES_USER WITH PASSWORD '$POSTGRES_PASSWORD';
EOSQL
