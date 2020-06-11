#!/usr/bin/env bash
set -ev

function validate() {
  if [[ -z "$1" ]]; then
    >&2 echo "Unable to find input: '$2'."
    exit 1
  fi
}

validate "$INPUT_GOOGLE_CLOUD_SERVICE_ACCOUNT" "googleCloudServiceAccount"

SERVICE_ACCOUNT_FILE=build/gcloud-key.json
echo "$INPUT_GOOGLE_CLOUD_SERVICE_ACCOUNT" > $SERVICE_ACCOUNT_FILE

export GOOGLE_APPLICATION_CREDENTIALS="$SERVICE_ACCOUNT_FILE"

java -jar "$FLANK_HOME/flank.jar" android run
