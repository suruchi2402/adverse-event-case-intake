#!/usr/bin/env bash
# Rebuilds out/ from fixtures/, the documents and the catalog. No API key and no network
# (once Maven has downloaded the dependencies).
#
#   ./replay.sh                       # committed catalog
#   ./replay.sh path/to/catalog.json  # a changed catalog: recorded values are re-checked against it;
#                                     # values that no longer pass, and fields the recordings do not cover, come out unknown with a note
#
# REPLAY_DOCS selects the documents folders (comma-separated). Add documents/stretch once its fixtures are recorded.
set -euo pipefail
cd "$(dirname "$0")"

CATALOG="${1:-catalog/field_catalog.json}"
DOCS="${REPLAY_DOCS:-documents}"

(cd backend && ./mvnw -q -DskipTests package)
java -jar backend/target/case-intake.jar extract \
  --provider=replay --docs="$DOCS" --catalog="$CATALOG" --out=out --fixtures=fixtures
