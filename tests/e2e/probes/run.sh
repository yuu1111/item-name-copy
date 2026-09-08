#!/usr/bin/env bash
set -euo pipefail

version=3.3.3
cache=/cache/gradle/e2e-probes
classes=/tmp/e2e-probe-classes
mkdir -p "$cache" "$classes"
classpath=$classes
case "$(uname -m)" in
    x86_64) native=natives-linux ;;
    aarch64) native=natives-linux-arm64 ;;
    *) echo 'Unsupported probe architecture' >&2; exit 1 ;;
esac
for module in lwjgl lwjgl-glfw; do
    for suffix in '' "-$native"; do
        jar="$module-$version$suffix.jar"
        if [[ ! -f "$cache/$jar" ]]; then
            curl --fail --location --retry 3 --silent --show-error \
                "https://repo.maven.apache.org/maven2/org/lwjgl/$module/$version/$jar" -o "$cache/$jar.tmp"
            mv "$cache/$jar.tmp" "$cache/$jar"
        fi
        classpath="$classpath:$cache/$jar"
    done
done
javac --release 8 -cp "$classpath" -d "$classes" tests/e2e/java/dev/e2e/driver/FileDriver.java tests/e2e/probes/GlfwInputProbe.java
java -cp "$classpath" GlfwInputProbe >"$E2E_ARTIFACTS/probe.log" 2>&1 || {
    cat "$E2E_ARTIFACTS/probe.log"
    exit 1
}
tail -n 1 "$E2E_ARTIFACTS/probe.log"
