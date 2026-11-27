#!/usr/bin/env bash
# One command: the review API from the committed outputs (no key, no model calls) and the review UI.
# Open http://localhost:5173 when Vite is ready. Ctrl+C stops both.
set -euo pipefail
cd "$(dirname "$0")"

(cd backend && ./mvnw -q -DskipTests package)
[ -d frontend/node_modules ] || (cd frontend && npm ci)

java -jar backend/target/case-intake.jar &
BACKEND_PID=$!
trap 'kill "$BACKEND_PID" 2>/dev/null || true' EXIT INT TERM

echo "Waiting for the API on http://localhost:8080 ..."
for _ in $(seq 1 60); do
  if curl -fs http://localhost:8080/api/catalog >/dev/null 2>&1; then break; fi
  if ! kill -0 "$BACKEND_PID" 2>/dev/null; then echo "The backend stopped; see the log above." >&2; exit 1; fi
  sleep 1
done

cd frontend && npm run dev
