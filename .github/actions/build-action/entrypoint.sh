#!/usr/bin/env bash

set -ev

GOOGLE_SERVICES_FILE=app/google-services.json
SERVICE_ACCOUNT_FILE=build/gcloud-key.json

if [ -z "$GOOGLE_SERVICES" ]; then
  echo "GOOGLE_SERVICES is required to generate $GOOGLE_SERVICES_FILE"
  exit 1
fi

if [ -z "$SERVICE_ACCOUNT" ]; then
  echo "SERVICE_ACCOUNT is required to generate $SERVICE_ACCOUNT_FILE"
  exit 1
fi

mkdir build
echo "$GOOGLE_SERVICES" > $GOOGLE_SERVICES_FILE
echo "$SERVICE_ACCOUNT" > $SERVICE_ACCOUNT_FILE

./gradlew build packageDebugAndroidTest

gcloud auth activate-service-account --key-file=$SERVICE_ACCOUNT_FILE
gcloud config set project sonar-colocate
gcloud firebase test android run \
  --type instrumentation \
  --app app/build/outputs/apk/debug/app-debug.apk \
  --test app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk \
  --device model=aljeter_n,version=26,locale=en,orientation=portrait \
  --use-orchestrator
