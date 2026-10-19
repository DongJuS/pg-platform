#!/bin/sh
set -eu
cd /workspace
gradle_tasks=""
while read -r service service_path service_port database_engine database_name; do
    case "$service" in ''|'#'*) continue ;; esac
    gradle_tasks="$gradle_tasks :$service:bootJar"
done < infra/docker/services.tsv
# These task names come only from the checked-in service registry.
# CI remains responsible for tests; building an image does not claim that tests passed.
# shellcheck disable=SC2086
sh ./gradlew --no-daemon --no-parallel --max-workers=2 \
    '-Dorg.gradle.jvmargs=-Xmx1024m -Dfile.encoding=UTF-8' $gradle_tasks
mkdir -p /artifacts
while read -r service service_path service_port database_engine database_name; do
    case "$service" in ''|'#'*) continue ;; esac
    jar_count=0
    for service_jar in "$service_path"/build/libs/*.jar; do
        case "$service_jar" in *-plain.jar) continue ;; esac
        [ -f "$service_jar" ] || continue
        cp "$service_jar" "/artifacts/$service.jar"
        jar_count=$((jar_count + 1))
    done
    [ "$jar_count" -eq 1 ] || { echo "Expected one bootJar for $service" >&2; exit 1; }
done < infra/docker/services.tsv
