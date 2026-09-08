#!/usr/bin/env bash
set -euo pipefail

last_id=0
while true; do
    if [[ ! -f "$E2E_CONTROL/request.json" ]]; then
        sleep 0.05
        continue
    fi
    request=$(cat "$E2E_CONTROL/request.json")
    id=$(jq -er '.id | select(type == "number" and . > 0 and floor == .)' <<<"$request")
    if ((id <= last_id)); then
        sleep 0.05
        continue
    fi
    last_id=$id
    printf '%s\n' "$request" >>"$E2E_ARTIFACTS/actions.jsonl"
    set +e
    response=$(timeout 15 bash /opt/e2e/x11-driver.sh "$request" 2>&1)
    status=$?
    set -e
    jq -n --argjson id "$id" --argjson exitCode "$status" --arg message "$response" \
        '{id: $id, exitCode: $exitCode, message: $message}' >"$E2E_CONTROL/response.tmp"
    cat "$E2E_CONTROL/response.tmp" >>"$E2E_ARTIFACTS/responses.jsonl"
    mv "$E2E_CONTROL/response.tmp" "$E2E_CONTROL/response.json"
done
