# scripts/ — CMN-F03 build & local deploy

Docker image is the deployment unit. The same immutable tag built here is what
will be pushed to ECR and run on ECS Fargate later.

| Script | Purpose |
| --- | --- |
| `build-images.sh` | Gradle bootJar → `docker build` (no push by default) |
| `local-deploy.sh` | canonical stack `up`: `infra/docker/compose.yaml` + `--profile apps`, `up -d --build --wait` |
| `local-teardown.sh` | `docker compose ... down` (volumes kept unless `--volumes`) |

## Roles: stack.sh vs scripts/local-*.sh

- `infra/docker/stack.sh` (owned by the canonical stack worker, do not edit) is
  a thin pass-through: `docker compose --env-file <env> -f infra/docker/compose.yaml "$@"`.
  Use it for day-to-day ad-hoc compose commands against the canonical stack.
- `scripts/local-deploy.sh` / `scripts/local-teardown.sh` (this directory) are
  opinionated wrappers around the same canonical stack: they default to the
  `apps` profile, pass `--build --wait`, attach the observability/localstack
  override in the correct merge order, guard volume deletion behind a typed
  confirmation, and document the ECR migration mapping below.

## Images: two build paths

The canonical compose file parameterizes images as
`${IMAGE_REGISTRY:-sinhan_project}/<service>:${IMAGE_TAG:-local}`. Both
variables pass straight through the wrappers from the shell environment.

- **(a) Default — compose `--build`.** `local-deploy.sh` builds via the
  multi-stage `infra/docker/Dockerfile` (compiles the jars inside Docker; no
  host JDK needed) and runs `sinhan_project/<service>:local`.
- **(b) Immutable tags — reuse `build-images.sh` output.** The images produced
  here (`pg-platform/<service>:<git short sha>`, via the single-stage
  `infra/docker/Dockerfile.service` packaging a Gradle-built bootJar) are the
  ones that will be pushed to ECR. To run them locally:

  ```sh
  IMAGE_REGISTRY=pg-platform IMAGE_TAG=<sha> scripts/local-deploy.sh --env-file /path/to/local.env --no-build
  ```

`build-images.sh` images are non-root, `JAVA_OPTS` with
`-XX:MaxRAMPercentage=65`, BusyBox-`wget` HEALTHCHECK on `/actuator/health`.

## Usage

Run from anywhere; the scripts `cd` to the repository root. Path (b) needs a
JDK 21 for the Gradle step — if `java` is not on PATH:

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```

```sh
# All 16 services, tagged pg-platform/<service>:<git short sha>
scripts/build-images.sh

# Subset only
scripts/build-images.sh transfer mock-pg

# Reuse existing jars, explicit tag
scripts/build-images.sh --skip-gradle --tag local transfer
```

The env file stays OUTSIDE the repository (README convention — never commit
credentials). Generate it once with `python3 infra/docker/init-env.py
/path/to/local.env`.

```sh
# Canonical stack, profile apps (16 app services; infra deps auto-start)
scripts/local-deploy.sh --env-file /path/to/local.env

# Opt-in overrides, repeatable, applied in CLI order after the base file —
# e.g. the outbox relay kit (enables OUTBOX_ENABLED=true on ledger/transfer;
# the base stack keeps the relay off by default on purpose):
scripts/local-deploy.sh --env-file /path/to/local.env \
    --override infra/docker/compose.outbox.yaml

# Add the observability override (alertmanager/tempo/loki) and/or localstack.
# Merge order rule: base → --override files (CLI order) → observability/localstack
# override ALWAYS LAST, regardless of flag order on the command line.
scripts/local-deploy.sh --env-file /path/to/local.env --observability
scripts/local-deploy.sh --env-file /path/to/local.env \
    --override infra/docker/compose.outbox.yaml --observability --localstack

# Dry run: resolve the merged compose config without starting anything
scripts/local-deploy.sh --env-file /path/to/local.env -- config --quiet

# Teardown (volumes kept). Pass the same override flags used at deploy time.
scripts/local-teardown.sh --env-file /path/to/local.env
scripts/local-teardown.sh --env-file /path/to/local.env --observability --localstack
scripts/local-teardown.sh --env-file /path/to/local.env \
    --override infra/docker/compose.outbox.yaml --observability
```

`--` replaces the default `up -d --build --wait` command entirely, so any
compose subcommand can be run through the wrapper with the same file/profile
combination.

Tag rules for `build-images.sh`: default is `git rev-parse --short HEAD`,
`local` outside a git checkout. `:latest` is never produced — mutable tags
break ECS rollback and auditability (immutable-tag principle).

## Outbox relay (opt-in)

`infra/docker/compose.outbox.yaml` is an opt-in override that sets
`OUTBOX_ENABLED=true` on `ledger` and `transfer`. The base stack keeps
`outbox.enabled: false` on purpose (canonical `application-docker.yml`:
"Docker startup must not silently start money flows"), so the relay only runs
when this override is explicitly added:

```sh
scripts/local-deploy.sh --env-file /path/to/local.env \
    --override infra/docker/compose.outbox.yaml
```

The OS env var outranks the config file via Spring relaxed binding;
`OutboxAutoConfiguration` additionally requires an `OutboxStore` bean
(skeleton profile registers one) and a `KafkaTemplate`
(`KAFKA_BOOTSTRAP_SERVERS=kafka:19092` is the base default; the five topics
come from canonical `init/kafka-topics.sh` — new topics go through the
canonical owner). Relay tuning env: `OUTBOX_POLL_DELAY_MS` (default 1000);
the lease(2m) > ackTimeout(5s) x batchSize(20) invariant is enforced in code.
Add more services to the override ONLY after that service's atomic write +
outbox_events recording is verified; until then the relay stays idle
(skeletons never write outbox rows), which is safe but pointless.

## AWS (ECR/ECS) migration map

| Now (local) | Later (AWS) |
| --- | --- |
| `build-images.sh` Gradle step | unchanged in CI |
| `docker build -t pg-platform/<svc>:<sha>` | same build, `IMAGE_REGISTRY=<acct>.dkr.ecr.<region>.amazonaws.com/pg-platform` |
| (nothing — push disabled) | `build-images.sh --push` after `aws ecr get-login-password`; CI has a disabled `ecr-push-ecs-deploy` job placeholder in `.github/workflows/build.yml` |
| `local-deploy.sh` (compose up on `infra/docker/compose.yaml`, project `sinhan_project_docker`) | `aws ecs update-service --force-new-deployment` per service with the same immutable tag; infra from `infra/terraform/envs/dev` (apply is a separate approval step) |
| env file outside repo | Secrets Manager per-service DB credentials, injected by the ECS task definition |
| compose `--build` (multi-stage `infra/docker/Dockerfile`) or `--no-build` with `IMAGE_REGISTRY`/`IMAGE_TAG` | CI builds jars and images; no build happens at deploy time |
| `PLATFORM` unset (host arch) | set `PLATFORM=linux/arm64` if the Terraform compute target is Graviton |

When switching registries, `IMAGE_REGISTRY` is the only build-time change; the
Dockerfile, jar contract and tags do not change.

## Notes / known gaps

- The compose files (`infra/docker/compose.yaml`, `compose.observability.yaml`)
  are the confirmed canonical stack (project `sinhan_project_docker`, profiles
  `apps`/`observability`/`localstack`) and are owned by the canonical stack
  worker — this directory never edits them. The old `infra/docker-compose/`
  stack is preserved but non-canonical; the wrappers no longer target it.
- `local-teardown.sh --volumes` deletes local DB volumes and asks for a typed
  confirmation; without it, data is retained.
