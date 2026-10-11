#!/usr/bin/env bash
# Starts a PostgreSQL instance just for this project: data in backend/.pgdata,
# port 5439, local connections only. It does not touch any other Postgres server.
#   scripts/local-db.sh          start (creates the database on first run)
#   scripts/local-db.sh stop     stop
set -euo pipefail
export LC_ALL="${LC_ALL:-en_US.UTF-8}"   # macOS: postgres refuses to start without a valid locale
cd "$(dirname "$0")/.."

PG_BIN="${PG_BIN:-$(dirname "$(command -v pg_ctl)")}"
DATA=".pgdata"
PORT=5439

if [[ "${1:-start}" == "stop" ]]; then
  "$PG_BIN/pg_ctl" -D "$DATA" stop
  exit 0
fi

if [[ ! -d "$DATA" ]]; then
  "$PG_BIN/initdb" -D "$DATA" -U hollis --auth=trust --encoding=UTF8 >/dev/null
  echo "listen_addresses = 'localhost'" >> "$DATA/postgresql.conf"
  echo "port = $PORT" >> "$DATA/postgresql.conf"
  "$PG_BIN/pg_ctl" -D "$DATA" -l "$DATA/server.log" -w start
  "$PG_BIN/createdb" -h localhost -p $PORT -U hollis hollis_park
  echo "Created database hollis_park on localhost:$PORT (user hollis)"
else
  "$PG_BIN/pg_ctl" -D "$DATA" status >/dev/null 2>&1 || "$PG_BIN/pg_ctl" -D "$DATA" -l "$DATA/server.log" -w start
fi
echo "PostgreSQL is running on localhost:$PORT"
