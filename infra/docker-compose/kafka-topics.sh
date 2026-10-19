#!/bin/sh
set -eu
# Event topics for the local platform. Auto-create is disabled on the broker
# (KAFKA_AUTO_CREATE_TOPICS_ENABLE=false), so topics are created here, idempotently.
# Names come from ADR 0001 (ledger.posted, transfer.completed) and the documented flows
# (transfer.outbound, fep.ack, payment.approved). libs/event-contracts defines no topic
# constants yet; revisit this list when the contracts land.
# Replication factor 1 is local-only.
for topic in ledger.posted transfer.completed transfer.outbound fep.ack payment.approved; do
    attempt=0
    until /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:19092 \
        --create --if-not-exists --topic "$topic" --partitions 3 --replication-factor 1; do
        attempt=$((attempt + 1))
        [ "$attempt" -lt 20 ] || exit 1
        sleep 3
    done
done
