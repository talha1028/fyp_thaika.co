#!/usr/bin/env bash
# Manual smoke test: verifies the Gemini REST endpoint is reachable and the
# key in local.properties is valid. Reads the key at runtime — never embeds
# it in this file, so this script is safe to commit even though the key isn't.
set -euo pipefail
cd "$(dirname "$0")/.."

KEY=$(grep '^GEMINI_API_KEY=' local.properties | cut -d'=' -f2-)
if [[ -z "$KEY" ]]; then
  echo "GEMINI_API_KEY not found in local.properties" >&2
  exit 1
fi

curl -sS -w "\nHTTP_STATUS:%{http_code}\n" \
  "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=${KEY}" \
  -H 'Content-Type: application/json' \
  -d '{"contents":[{"parts":[{"text":"Reply with exactly: pong"}]}]}'
