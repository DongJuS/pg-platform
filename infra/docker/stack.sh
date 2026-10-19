#!/bin/sh
set -eu
if [ "$#" -lt 2 ]; then
    echo "Usage: sh infra/docker/stack.sh /absolute/path/to/local.env [compose options] COMMAND [services...]" >&2
    exit 2
fi
docker_env_file=$1
shift
case "$docker_env_file" in /*) ;; *) echo "Use an absolute env-file path." >&2; exit 2 ;; esac
[ -f "$docker_env_file" ] || { echo "Env file not found: $docker_env_file" >&2; exit 2; }
docker_dir=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
# Do not source/eval the env file. Compose reads it as data.
exec docker compose --env-file "$docker_env_file" -f "$docker_dir/compose.yaml" "$@"
