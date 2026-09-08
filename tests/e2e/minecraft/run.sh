#!/usr/bin/env bash
set -euo pipefail

target=${1:-1.21.1-fabric}
if [[ ! "$target" =~ ^[0-9][0-9A-Za-z.-]*$ || ! -f "versions/$target/gradle.properties" ]]; then
    echo "Unknown Minecraft target: $target" >&2
    exit 1
fi
source_hash=$(find src core/src gradle minecraft-client-testkit tests/client tests/e2e -type f \
    -not -path '*/build/*' -not -path '*/.gradle/*' -print0 | sort -z | xargs -0 sha256sum | sha256sum | cut -d' ' -f1)
report="versions/$target/build/reports/client-test"
collect() {
    if [[ -d "$report" ]]; then cp -a "$report" "$E2E_ARTIFACTS/client-report"; fi
    if [[ -d "versions/$target/build/client-test/run/logs" ]]; then
        cp -a "versions/$target/build/client-test/run/logs" "$E2E_ARTIFACTS/client-logs"
    fi
}
trap collect EXIT
set +e
bash gradlew "-Ptarget=$target" "-PclientTestSource=$source_hash" -PclientTestInput=external \
    '-Dorg.gradle.jvmargs=-Xmx2G' --max-workers=2 --no-daemon ":$target:runClient" --console=plain \
    >"$E2E_ARTIFACTS/minecraft.log" 2>&1
status=$?
set -e
if ((status != 0)); then
    tail -n 60 "$E2E_ARTIFACTS/minecraft.log"
    exit "$status"
fi
jq -e --arg target "$target" --arg source "$source_hash" \
    '.target == $target and .source == $source and .mode == "real client, external X11 input, OS clipboard" and .failed == 0 and .passed == 3 and .expectedTests == 3' \
    "$report/results.json" >/dev/null
echo "PASS $target: 3 tests with external X11 input"
