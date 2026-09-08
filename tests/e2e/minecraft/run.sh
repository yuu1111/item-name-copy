#!/usr/bin/env bash
set -euo pipefail

target=${1:-1.21.1-fabric}
input=${2:-external}
case "$input" in
    external) expected=3; mode='real client, external X11 input, OS clipboard' ;;
    synthetic) expected=13; mode='real client, synthetic input callbacks, OS clipboard' ;;
    *) echo "Unknown input suite: $input" >&2; exit 1 ;;
esac
if [[ ! "$target" =~ ^[0-9][0-9A-Za-z.-]*$ || ! -f "versions/$target/gradle.properties" ]]; then
    echo "Unknown Minecraft target: $target" >&2
    exit 1
fi
source_hash=$({
    find src core/src gradle minecraft-client-testkit tests/client tests/e2e -type f \
        -not -path '*/build/*' -not -path '*/.gradle/*' -print0
    find . -maxdepth 1 -type f \( -name '*.gradle.kts' -o -name 'gradle.properties' \) -print0
    printf '%s\0' "versions/$target/gradle.properties"
} | sort -z | xargs -0 sha256sum | sha256sum | cut -d' ' -f1)
report="versions/$target/build/reports/client-test"
collect() {
    if [[ -d "$report" ]]; then cp -a "$report" "$E2E_ARTIFACTS/client-report"; fi
    if [[ -d "versions/$target/build/client-test/run/logs" ]]; then
        cp -a "versions/$target/build/client-test/run/logs" "$E2E_ARTIFACTS/client-logs"
    fi
}
trap collect EXIT
set +e
bash gradlew "-Ptarget=$target" "-PclientTestSource=$source_hash" "-PclientTestInput=$input" \
    '-Dorg.gradle.jvmargs=-Xmx2G' --max-workers=2 --no-daemon ":$target:runClient" --console=plain \
    >"$E2E_ARTIFACTS/minecraft.log" 2>&1
status=$?
set -e
if ((status != 0)); then
    tail -n 60 "$E2E_ARTIFACTS/minecraft.log"
    exit "$status"
fi
jq -e --arg target "$target" --arg source "$source_hash" --arg mode "$mode" --argjson expected "$expected" \
    '.target == $target and .source == $source and .mode == $mode and .failed == 0 and .passed == $expected and .expectedTests == $expected' \
    "$report/results.json" >/dev/null
echo "PASS $target: $expected tests ($input)"
