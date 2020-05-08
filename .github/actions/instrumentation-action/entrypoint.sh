#!/usr/bin/env bash

set -ev

function validate() {
  if [[ -z "$1" ]]; then
    >&2 echo "Unable to find input: '$2'."
    exit 1
  fi
}

validate "$INPUT_GOOGLE_CLOUD_SERVICE_ACCOUNT" "Google Cloud Service Account"

SERVICE_ACCOUNT_FILE=build/gcloud-key.json
echo "$INPUT_GOOGLE_CLOUD_SERVICE_ACCOUNT" > $SERVICE_ACCOUNT_FILE

gcloud auth activate-service-account --key-file=$SERVICE_ACCOUNT_FILE
gcloud config set project sonar-colocate
gcloud firebase test android run \
  --type instrumentation \
  --app app/build/outputs/apk/debug/app-debug.apk \
  --test app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk \
  --device model=aljeter_n,version=26,locale=en,orientation=portrait \
  --use-orchestrator \
  --environment-variables clearPackageData=true
