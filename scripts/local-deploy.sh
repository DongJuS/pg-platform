#!/usr/bin/env bash
# scripts/local-deploy.sh — start the canonical local platform stack.
#
# Opinionated wrapper around the canonical stack (owned by infra/docker,
# maintained by another worker — do not edit those files here):
#   docker compose -f infra/docker/compose.yaml \
#     [-f <each --override file, in CLI order>] \
#     [--observability/--localstack add -f infra/docker/compose.observability.yaml
#       ALWAYS LAST, after the base file and any --override files] \
#     --env-file <env file> --profile apps up -d --build --wait
#
# Project name comes from the compose file itself: sinhan_project_docker.
# The env file lives OUTSIDE the repository (never commit credentials).
# Generate it once with: python3 infra/docker/init-env.py /path/to/local.env
#
# Usage:
#   scripts/local-deploy.sh --env-file /path/to/local.env [options] [-- extra compose args]
#     --env-file FILE   env file with POSTGRES_PASSWORD etc. (required, outside the repo)
#     -f FILE           replace the base compose file (default: infra/docker/compose.yaml)
#     --profile P       add a profile (default profiles: apps)
#     --override FILE   append an extra compose override file (repeatable, applied
#                       in CLI order after the base file); e.g. the opt-in outbox
#                       relay kit: --override infra/docker/compose.outbox.yaml
#     --observability   append compose.observability.yaml AFTER the base file and
#                       any --override files (always last) and add --profile
#                       observability (alertmanager/tempo/loki)
#     --localstack      append compose.observability.yaml after the base file and
#                       any --override files (always last) and add --profile
#                       localstack (S3 substitute)
#     --no-build        do not pass --build (reuse existing images as-is)
#     -- ARGS           replace the `up -d --build --wait` command entirely, e.g.
#                       a dry run:  scripts/local-deploy.sh --env-file ... -- config --quiet
#
# Image tags: the canonical compose file reads ${IMAGE_REGISTRY:-sinhan_project}
# and ${IMAGE_TAG:-local}; both pass straight through from this script's
# environment. Two image paths:
#   (a) default: compose --build uses the multi-stage infra/docker/Dockerfile
#       (no host JDK needed) and tags sinhan_project/<svc>:local.
#   (b) immutable tags: reuse scripts/build-images.sh output pg-platform/<svc>:<sha>
#       with IMAGE_REGISTRY=pg-platform IMAGE_TAG=<sha> scripts/local-deploy.sh ... --no-build
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

BASE_COMPOSE_FILE="infra/docker/compose.yaml"
OBS_OVERRIDE="infra/docker/compose.observability.yaml"
OVERRIDE_ACTIVE=0
ENV_FILE=""
PROFILES=(apps)
BUILD_FLAG="--build"
EXTRA=()
USER_OVERRIDES=()

add_profile() {
    local p="$1" existing
    for existing in "${PROFILES[@]}"; do
        [ "$existing" = "$p" ] && return 0
    done
    PROFILES+=("$p")
}

while [ $# -gt 0 ]; do
    case "$1" in
        --env-file) ENV_FILE="$2"; shift 2 ;;
        -f) BASE_COMPOSE_FILE="$2"; shift 2 ;;
        --profile) add_profile "$2"; shift 2 ;;
        --override) USER_OVERRIDES+=("$2"); shift 2 ;;
        --observability) OVERRIDE_ACTIVE=1; add_profile observability; shift ;;
        --localstack) OVERRIDE_ACTIVE=1; add_profile localstack; shift ;;
        --no-build) BUILD_FLAG=""; shift ;;
        --) shift; EXTRA=("$@"); break ;;
        *) echo "Unknown option: $1" >&2; exit 2 ;;
    esac
done

[ -n "$ENV_FILE" ] || { echo "Required: --env-file /path/to/local.env (kept outside the repo)" >&2; exit 2; }
[ -f "$ENV_FILE" ] || { echo "Env file not found: $ENV_FILE" >&2; exit 2; }
[ -f "$BASE_COMPOSE_FILE" ] || { echo "Compose file not found: $BASE_COMPOSE_FILE" >&2; exit 2; }

# Merge order matters: the base file first, then --override files in CLI order,
# and the observability/localstack override strictly last.
FILE_ARGS=(-f "$BASE_COMPOSE_FILE")
for ov in "${USER_OVERRIDES[@]:-}"; do
    [ -n "$ov" ] || continue
    [ -f "$ov" ] || { echo "Override file not found: $ov" >&2; exit 2; }
    FILE_ARGS+=(-f "$ov")
done
if [ "$OVERRIDE_ACTIVE" -eq 1 ]; then
    [ -f "$OBS_OVERRIDE" ] || { echo "Override file not found: $OBS_OVERRIDE" >&2; exit 2; }
    FILE_ARGS+=(-f "$OBS_OVERRIDE")
fi

PROFILE_ARGS=()
for p in "${PROFILES[@]}"; do PROFILE_ARGS+=(--profile "$p"); done

echo ">> docker compose ${FILE_ARGS[*]} --env-file $ENV_FILE ${PROFILE_ARGS[*]}" \
    "(IMAGE_REGISTRY=${IMAGE_REGISTRY:-sinhan_project} IMAGE_TAG=${IMAGE_TAG:-local})"

if [ "${#EXTRA[@]}" -gt 0 ]; then
    # Pass-through path (e.g. dry run: `-- config --quiet`) — no containers start.
    docker compose "${FILE_ARGS[@]}" --env-file "$ENV_FILE" \
        "${PROFILE_ARGS[@]}" \
        "${EXTRA[@]}"
else
    echo ">> up -d ${BUILD_FLAG:+$BUILD_FLAG} --wait"
    docker compose "${FILE_ARGS[@]}" --env-file "$ENV_FILE" \
        "${PROFILE_ARGS[@]}" \
        up -d $BUILD_FLAG --wait
fi
