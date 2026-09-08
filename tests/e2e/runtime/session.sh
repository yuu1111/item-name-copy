#!/usr/bin/env bash
set -euo pipefail

run_id=${E2E_RUN_ID:-$(date -u +%Y%m%dT%H%M%S%N)}
if [[ ! "$run_id" =~ ^[A-Za-z0-9_-]+$ ]]; then
    echo 'Invalid E2E_RUN_ID' >&2
    exit 1
fi
mkdir -p "$E2E_ARTIFACTS" "$E2E_CONTROL"
export E2E_ARTIFACTS="$E2E_ARTIFACTS/$run_id"
export E2E_CONTROL="$E2E_CONTROL/$run_id"
mkdir "$E2E_ARTIFACTS" "$E2E_CONTROL"
started=$(date +%s)
children=()
cleanup() {
    local status=$?
    trap - EXIT
    jq -n --argjson exitCode "$status" --argjson durationSeconds "$(($(date +%s) - started))" \
        --arg runId "$run_id" '{schemaVersion: 1, runId: $runId, exitCode: $exitCode, durationSeconds: $durationSeconds}' \
        >"$E2E_ARTIFACTS/run.json"
    for child in "${children[@]}"; do
        kill "$child" 2>/dev/null || true
    done
    wait 2>/dev/null || true
    echo "Artifacts: $E2E_ARTIFACTS"
    exit "$status"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

Xvfb "$DISPLAY" -screen 0 1280x720x24 -nolisten tcp >"$E2E_ARTIFACTS/xvfb.log" 2>&1 &
children+=("$!")
ready=false
for ((attempt = 0; attempt < 100; attempt++)); do
    if xdpyinfo >/dev/null 2>&1; then
        ready=true
        break
    fi
    sleep 0.1
done
if [[ "$ready" != true ]]; then
    echo 'X11 display did not become ready' >&2
    exit 1
fi
openbox --sm-disable >"$E2E_ARTIFACTS/window-manager.log" 2>&1 &
children+=("$!")
{
    uname -a
    java -version 2>&1
    xdotool --version
    glxinfo -B
    dpkg-query -W xvfb xdotool xclip libgl1-mesa-dri
} >"$E2E_ARTIFACTS/environment.txt"

bash /opt/e2e/serve-x11.sh >"$E2E_ARTIFACTS/driver.log" 2>&1 &
children+=("$!")
timeout --kill-after=10 "${E2E_TIMEOUT_SECONDS:-600}" "$@" &
application=$!
children+=("$application")
set +e
wait -n -p finished "${children[@]}"
status=$?
set -e
if [[ "${finished:-}" != "$application" ]]; then
    echo "Runtime service exited before the application: ${finished:-unknown}, status $status" >&2
    exit 1
fi
exit "$status"
