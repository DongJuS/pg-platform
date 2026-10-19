#!/usr/bin/env bash
# scripts/build-images.sh — build service bootJars with Gradle, then package
# Docker images per the compose contract:
#   docker build -f infra/docker/Dockerfile.service --build-arg JAR_PATH=... \
#     -t pg-platform/<service>:<tag> <repo root>
#
# Immutable-tag principle: images are tagged with the git short SHA (or `local`
# outside a git checkout). `:latest` is never produced on purpose; mutable tags
# make ECS rollbacks and audit trails unreliable. This is the exact tag that
# will be pushed to ECR later.
#
# Usage:
#   scripts/build-images.sh [options] [service ...]
#     service ...       subset of services.tsv names (default: all 16)
#     --tag TAG         image tag (default: git short SHA, else "local")
#     --skip-gradle     reuse existing build/libs jars (do not run bootJar)
#     --push            docker push each tag (future ECR step; needs a registry
#                       login done beforehand — this script never calls AWS)
#   Env:
#     IMAGE_REGISTRY    default "pg-platform" (later: <account>.dkr.ecr.<region>.amazonaws.com/pg-platform)
#     PLATFORM          optional docker build --platform value (e.g. linux/arm64 for ECS Fargate)
#     JAVA_HOME         must point to a JDK 21 if `java` is not on PATH, e.g.
#                       export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

REGISTRY_FILE="infra/docker/services.tsv"
DOCKERFILE="infra/docker/Dockerfile.service"
IMAGE_REGISTRY="${IMAGE_REGISTRY:-pg-platform}"
SKIP_GRADLE=0
PUSH=0
TAG=""
SELECTED=()

while [ $# -gt 0 ]; do
    case "$1" in
        --tag) TAG="$2"; shift 2 ;;
        --skip-gradle) SKIP_GRADLE=1; shift ;;
        --push) PUSH=1; shift ;;
        -h|--help) sed -n '1,30p' "$0"; exit 0 ;;
        -*) echo "Unknown option: $1" >&2; exit 2 ;;
        *) SELECTED+=("$1"); shift ;;
    esac
done

# --- service registry -------------------------------------------------------
# services.tsv columns: service, module path, port, db engine, db name.
SERVICES=()
PATHS=()
while read -r service service_path _rest; do
    case "$service" in ''|'#'*) continue ;; esac
    SERVICES+=("$service")
    PATHS+=("$service_path")
done < "$REGISTRY_FILE"

if [ "${#SELECTED[@]}" -gt 0 ]; then
    # Keep only requested services, preserving registry order.
    KEEP=()
    for s in "${SELECTED[@]}"; do KEEP+=("$s"); done
    SELECTED=()
    for i in "${!SERVICES[@]}"; do
        for s in "${KEEP[@]}"; do
            if [ "${SERVICES[$i]}" = "$s" ]; then
                SELECTED+=("${SERVICES[$i]}")
            fi
        done
    done
    [ "${#SELECTED[@]}" -eq "${#KEEP[@]}" ] || { echo "Unknown service name in: ${KEEP[*]}" >&2; exit 2; }
else
    SELECTED=("${SERVICES[@]}")
fi

# --- tag --------------------------------------------------------------------
if [ -z "$TAG" ]; then
    TAG="$(git rev-parse --short HEAD 2>/dev/null || echo local)"
fi
echo ">> Image tag: $TAG (registry prefix: $IMAGE_REGISTRY)"

# --- Gradle bootJars ----------------------------------------------------------
jar_path() {  # $1 = service name -> echoes expected bootJar path
    for i in "${!SERVICES[@]}"; do
        if [ "${SERVICES[$i]}" = "$1" ]; then
            echo "${PATHS[$i]}/build/libs/$1-0.1.0-SNAPSHOT.jar"
            return 0
        fi
    done
    return 1
}

if [ "$SKIP_GRADLE" -eq 0 ]; then
    if ! command -v java >/dev/null 2>&1 && [ -z "${JAVA_HOME:-}" ]; then
        echo "No java on PATH and JAVA_HOME unset. On this machine:" >&2
        echo "  export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" >&2
        exit 1
    fi
    TASKS=()
    for s in "${SELECTED[@]}"; do TASKS+=(":$s:bootJar"); done
    echo ">> ./gradlew ${TASKS[*]} -x test"
    ./gradlew --no-daemon "${TASKS[@]}" -x test
fi

# --- docker build -------------------------------------------------------------
BUILT=()
for s in "${SELECTED[@]}"; do
    JAR="$(jar_path "$s")"
    if [ ! -f "$JAR" ]; then
        echo "Missing bootJar: $JAR (run without --skip-gradle)" >&2
        exit 1
    fi
    IMAGE="$IMAGE_REGISTRY/$s:$TAG"
    echo ">> docker build -t $IMAGE  (JAR_PATH=$JAR)"
    EXTRA=()
    [ -n "${PLATFORM:-}" ] && EXTRA+=(--platform "$PLATFORM")
    docker build ${EXTRA[@]+"${EXTRA[@]}"} \
        -f "$DOCKERFILE" \
        --build-arg "JAR_PATH=$JAR" \
        -t "$IMAGE" \
        "$REPO_ROOT"
    BUILT+=("$IMAGE")
    if [ "$PUSH" -eq 1 ]; then
        echo ">> docker push $IMAGE"
        docker push "$IMAGE"
    fi
done

echo ""
echo "Built ${#BUILT[@]} image(s):"
printf '  %s\n' "${BUILT[@]}"
