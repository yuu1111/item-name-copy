#!/usr/bin/env bash
set -euo pipefail

mkdir -p "$E2E_ARTIFACTS" "$E2E_CONTROL"
children=()
cleanup() {
    for child in "${children[@]}"; do
        kill "$child" 2>/dev/null || true
    done
    wait 2>/dev/null || true
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
"$@" &
children+=("$!")
set +e
wait "${children[-1]}"
status=$?
set -e
exit "$status"
