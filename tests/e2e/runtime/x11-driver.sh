#!/usr/bin/env bash
set -euo pipefail

request=$1
action=$(jq -er '.action' <<<"$request")
pid=$(jq -er '.pid | select(type == "number" and . > 0 and floor == .)' <<<"$request")
mapfile -t windows < <(xdotool search --onlyvisible --pid "$pid")
if ((${#windows[@]} != 1)); then
    echo "Expected one visible window for pid $pid; found ${#windows[@]}" >&2
    exit 1
fi
window=${windows[0]}
xdotool windowactivate --sync "$window"
case "$action" in
    hover)
        x=$(jq -er '.x | select(type == "number" and . >= 0 and floor == .)' <<<"$request")
        y=$(jq -er '.y | select(type == "number" and . >= 0 and floor == .)' <<<"$request")
        xdotool mousemove --sync --window "$window" "$x" "$y"
        ;;
    hotkey)
        keys=$(jq -er '.keys | select(type == "string" and test("^[A-Za-z0-9_+]+$"))' <<<"$request")
        trap 'xdotool keyup Control_L Control_R Shift_L Shift_R Alt_L Alt_R Super_L Super_R c 2>/dev/null || true' EXIT
        xdotool key --clearmodifiers --delay 80 "$keys"
        ;;
    clipboard)
        expected=$(jq -er '.expected | select(type == "string")' <<<"$request")
        actual=$(timeout 3 xclip -selection clipboard -o)
        if [[ "$actual" != "$expected" ]]; then
            printf 'Clipboard mismatch: expected <%s>, actual <%s>\n' "$expected" "$actual" >&2
            exit 1
        fi
        printf '%s\n' "$actual"
        ;;
    screenshot)
        id=$(jq -er '.id | select(type == "number" and . > 0 and floor == .)' <<<"$request")
        import -window "$window" "$E2E_ARTIFACTS/screen-$id.png"
        ;;
    *)
        echo "Unsupported X11 action: $action" >&2
        exit 1
        ;;
esac
