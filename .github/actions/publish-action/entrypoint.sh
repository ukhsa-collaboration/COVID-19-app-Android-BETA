#!/usr/bin/env bash

set -ev

CURRENT_DIR=$(pwd)

GOOGLE_SERVICES_FILE=app/google-services.json
PLAY_SERVICES_FILE=$CURRENT_DIR/build/play-services.json
SIGNING_KEY_STORE_FILE=$CURRENT_DIR/build/keystore

if [ -z "$PLAY_TRACK" ]; then
  echo "PLAY_TRACK is required"
  exit 1
fi

if [ -z "$GOOGLE_SERVICES" ]; then
  echo "GOOGLE_SERVICES is required to generate $GOOGLE_SERVICES_FILE"
  exit 1
fi

if [ -z "$PLAY_SERVICES" ]; then
  echo "PLAY_SERVICES is required"
  exit 1
fi

if [ -z "$SIGNING_KEY_STORE" ]; then
  echo "SIGNING_KEY_STORE is required"
  exit 1
fi

if [ -z "$SIGNING_KEY_ALIAS" ]; then
  echo "SIGNING_KEY_ALIAS is required"
  exit 1
fi

if [ -z "$SIGNING_KEY_STORE_PASSWORD" ]; then
  echo "SIGNING_KEY_STORE_PASSWORD is required"
  exit 1
fi

if [ -z "$SIGNING_KEY_PASSWORD" ]; then
  echo "SIGNING_KEY_PASSWORD is required"
  exit 1
fi

if [ -z "$SONAR_HEADER_VALUE" ]; then
  echo "SONAR_HEADER_VALUE is required"
  exit 1
fi

if [ -z "$SONAR_ANALYTICS_KEY" ]; then
  echo "SONAR_ANALYTICS_KEY is required"
  exit 1
fi

mkdir -p build
echo "$GOOGLE_SERVICES" > $GOOGLE_SERVICES_FILE
echo "$PLAY_SERVICES" > "$PLAY_SERVICES_FILE"

echo "$SIGNING_KEY_STORE" > build/keystore.txt
base64 -d build/keystore.txt > "$SIGNING_KEY_STORE_FILE"

./gradlew "publish$BUILD_FLAVOR" --track="$PLAY_TRACK" \
   -Pgitcommit="${COMMIT_SHA:0:7}" \
   -Psonar.headerValue="$SONAR_HEADER_VALUE" \
   -Psonar.analyticsKey="$SONAR_ANALYTICS_KEY" \
   -Psonar.baseUrl="$SONAR_BASE_URL" \
   -Pplay-enabled \
   -PPLAY_SERVICES_PUBLISH="$PLAY_SERVICES_FILE" \
   -PSIGNING_KEY_STORE="$SIGNING_KEY_STORE_FILE" \
   -PSIGNING_KEY_ALIAS="$SIGNING_KEY_ALIAS" \
   -PSIGNING_KEY_STORE_PASSWORD="$SIGNING_KEY_STORE_PASSWORD" \
   -PSIGNING_KEY_PASSWORD="$SIGNING_KEY_PASSWORD" \

# Return Build Version
BUILD_VERSION_FILE=app/build/version.txt
BUILD_VERSION=$(head -n 1 $BUILD_VERSION_FILE)
echo "::set-output name=buildVersion::$BUILD_VERSION"
