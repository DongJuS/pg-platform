#!/bin/sh
set -eu
# Known names from ADR 0001 and the existing flow descriptions. Replication=1 is local-only.
# TODO[CONTRACT]: finalize partitions, retention, keys, ACLs and any additional event topics.
for topic in ledger.posted ledger.overseas-applied transfer.completed transfer.outbound fep.ack payment.approved payment.refunded payment.cancelled recon.mismatch; do
    attempt=0
    until /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:19092 \
        --create --if-not-exists --topic "$topic" --partitions 3 --replication-factor 1; do
        attempt=$((attempt + 1))
        [ "$attempt" -lt 20 ] || exit 1
        sleep 3
    done
done
