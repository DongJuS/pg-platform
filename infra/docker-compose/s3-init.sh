#!/bin/sh
set -eu
# Creates the local S3 report bucket for wealth-report on localstack. Idempotent.
# The AWS bucket name is "<prefix>-<account_id>-reports" (infra/terraform/modules/storage);
# the local default is fixed in compose.yaml as REPORT_BUCKET=pg-platform-local-reports.
: "${REPORT_BUCKET:?REPORT_BUCKET is required}"
ENDPOINT=${LOCALSTACK_ENDPOINT:-http://localstack:4566}
attempt=0
until aws --endpoint-url="$ENDPOINT" s3api head-bucket --bucket "$REPORT_BUCKET" 2>/dev/null; do
    aws --endpoint-url="$ENDPOINT" s3 mb "s3://$REPORT_BUCKET" && break
    attempt=$((attempt + 1))
    [ "$attempt" -lt 20 ] || exit 1
    sleep 3
done
echo "s3 bucket ready: $REPORT_BUCKET"
