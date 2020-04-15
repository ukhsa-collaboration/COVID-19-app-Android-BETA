#!/usr/bin/env bash

set -ev

CURRENT_DIR=$(pwd)

FIREBASE_SERVICES_FILE=app/google-services.json
PLAY_SERVICES_FILE=$CURRENT_DIR/build/play-services.json
SIGNING_KEY_STORE_FILE=$CURRENT_DIR/build/keystore

if [ -z "$FIREBASE_SERVICES" ]; then
  echo "FIREBASE_SERVICES is required to generate $FIREBASE_SERVICES_FILE"
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

mkdir build
echo "$FIREBASE_SERVICES" > $FIREBASE_SERVICES_FILE
echo "$PLAY_SERVICES" > $PLAY_SERVICES_FILE

echo "$SIGNING_KEY_STORE" > build/keystore.txt
base64 -d build/keystore.txt > $SIGNING_KEY_STORE_FILE

./gradlew publish  -Pplay-enabled \
   -PPLAY_SERVICES_PUBLISH=$PLAY_SERVICES_FILE \
   -PSIGNING_KEY_STORE=$SIGNING_KEY_STORE_FILE \
   -PSIGNING_KEY_ALIAS="$SIGNING_KEY_ALIAS" \
   -PSIGNING_KEY_STORE_PASSWORD="$SIGNING_KEY_STORE_PASSWORD" \
   -PSIGNING_KEY_PASSWORD="$SIGNING_KEY_PASSWORD" \
