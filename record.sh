#!/usr/bin/env bash
# Runs the pipeline against the live model and records every exchange under fixtures/.
# Needs GEMINI_API_KEY and GEMINI_MODEL in the environment (never in a file).
# Documents that already have a recording for their exact text are answered from it; pass --refresh to call again.
#
#   export GEMINI_API_KEY=...   GEMINI_MODEL=...      # model id copied from Google AI Studio
#   ./record.sh                                       # core documents
#   ./record.sh --docs=documents,documents/stretch    # core and stretch documents
#   ./record.sh --refresh                             # after a prompt change: call the model again for every document
set -euo pipefail
cd "$(dirname "$0")"

: "${GEMINI_API_KEY:?Set GEMINI_API_KEY in your shell (not in a file)}"
: "${GEMINI_MODEL:?Set GEMINI_MODEL to the exact model id from Google AI Studio}"

ARGS=("$@")
if [[ ! " ${ARGS[*]-} " =~ " --docs=" ]]; then
  ARGS+=("--docs=documents")
fi

(cd backend && ./mvnw -q -DskipTests package)
java -jar backend/target/case-intake.jar extract --provider=gemini --catalog=catalog/field_catalog.json \
  --out=out --fixtures=fixtures "${ARGS[@]}"

python3 tools/check.py --catalog catalog/field_catalog.json --answers answers --out out --docs documents --quiet
