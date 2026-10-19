#!/usr/bin/env bash
# scripts/local-teardown.sh — stop the canonical local platform stack started by
# scripts/local-deploy.sh.
#
# Default: `docker compose ... down` — containers and networks are removed,
# named volumes (database data) are KEPT. Existing data is never deleted
# unless you explicitly pass --volumes.
#
# Pass the same --override/--observability/--localstack flags used at deploy
# time so the override services are part of the model and their containers are
# removed too. Merge order is identical to deploy: base → --override files in
# CLI order → observability/localstack override last.
#
# Usage:
#   scripts/local-teardown.sh --env-file /path/to/local.env [options]
#     --env-file FILE   env file used at deploy time (required)
#     -f FILE           replace the base compose file (default: infra/docker/compose.yaml)
#     --profile P       add a profile (default profiles: apps)
#     --override FILE   append an extra compose override file (repeatable, applied
#                       in CLI order after the base file); pass the same overrides
#                       used at deploy time
#     --observability   append compose.observability.yaml after the base file and
#                       any --override files (always last) and add --profile observability
#     --localstack      append compose.observability.yaml after the base file and
#                       any --override files (always last) and add --profile localstack
#     --volumes         ALSO delete named volumes (DESTRUCTIVE: drops local DB data)
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

BASE_COMPOSE_FILE="infra/docker/compose.yaml"
OBS_OVERRIDE="infra/docker/compose.observability.yaml"
OVERRIDE_ACTIVE=0
ENV_FILE=""
PROFILES=(apps)
VOLUMES_FLAG=""
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
        --volumes) VOLUMES_FLAG="--volumes"; shift ;;
        *) echo "Unknown option: $1" >&2; exit 2 ;;
    esac
done

[ -n "$ENV_FILE" ] || { echo "Required: --env-file /path/to/local.env" >&2; exit 2; }
[ -f "$BASE_COMPOSE_FILE" ] || { echo "Compose file not found: $BASE_COMPOSE_FILE" >&2; exit 2; }

# Same merge order as deploy: base first, then --override files in CLI order,
# observability/localstack override strictly last, so the down targets the exact
# service/volume set that was started.
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

if [ -n "$VOLUMES_FLAG" ]; then
    echo "WARNING: --volumes deletes ALL named volumes of this compose project (local DB data)." >&2
    echo "Type 'delete-volumes' to continue: " >&2
    read -r answer
    [ "$answer" = "delete-volumes" ] || { echo "Aborted." >&2; exit 1; }
fi

echo ">> docker compose ${FILE_ARGS[*]} --env-file $ENV_FILE ${PROFILE_ARGS[*]} down $VOLUMES_FLAG"
docker compose "${FILE_ARGS[@]}" --env-file "$ENV_FILE" \
    "${PROFILE_ARGS[@]}" \
    down $VOLUMES_FLAG
