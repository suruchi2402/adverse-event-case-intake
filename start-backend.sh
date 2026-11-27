#!/usr/bin/env bash
# Starts the review API on http://localhost:8080 from the committed files (out/, documents/, catalog/). No key needed.
set -euo pipefail
cd "$(dirname "$0")"
(cd backend && ./mvnw -q -DskipTests package)
exec java -jar backend/target/case-intake.jar
