#!/usr/bin/env bash

set -ev

GOOGLE_SERVICES_FILE=app/google-services.json

if [ -z "$GOOGLE_SERVICES" ]; then
  echo "GOOGLE_SERVICES is required to generate $GOOGLE_SERVICES_FILE"
  exit 1
fi

mkdir build
echo "$GOOGLE_SERVICES" > $GOOGLE_SERVICES_FILE

./gradlew build packageDebugAndroidTest -Psonar.headerValue="$TEST_SONAR_HEADER_VALUE" -Psonar.analyticsKey="$SONAR_ANALYTICS_KEY"
