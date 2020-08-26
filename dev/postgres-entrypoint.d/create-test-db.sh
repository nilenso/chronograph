#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
     CREATE USER chronograph_test WITH PASSWORD 'chronograph_testpwd';
     CREATE DATABASE chronograph_test;
     GRANT ALL PRIVILEGES ON DATABASE chronograph_test TO chronograph_test;
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "chronograph_test" <<-EOSQL
     ALTER SCHEMA information_schema OWNER TO chronograph_test;
     ALTER SCHEMA public OWNER TO chronograph_test;
     ALTER SCHEMA pg_catalog OWNER TO chronograph_test;
EOSQL
