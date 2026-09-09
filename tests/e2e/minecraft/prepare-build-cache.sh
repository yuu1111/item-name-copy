#!/usr/bin/env bash

project_cache="${GRADLE_USER_HOME:?}/itemnamecopy-workspace-v1"
mkdir -p "$project_cache"
exec 9>"$project_cache/lock"
if ! flock -n 9; then
    echo 'Another Minecraft test is using the build cache' >&2
    exit 1
fi

for project in . core minecraft-client-testkit tests/client versions/*; do
    [[ -d "$project" ]] || continue
    for name in .gradle build; do
        destination="$project_cache/$project/$name"
        local_path="$project/$name"
        mkdir -p "$destination"
        if [[ -L "$local_path" && "$(readlink -f "$local_path")" == "$(readlink -f "$destination")" ]]; then
            continue
        fi
        if [[ -d "$local_path" && ! -L "$local_path" ]]; then
            rmdir "$local_path" || { echo "Build directory is not empty: $local_path" >&2; exit 1; }
        fi
        ln -s "$destination" "$local_path"
    done
done
