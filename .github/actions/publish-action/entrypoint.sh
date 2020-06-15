#!/usr/bin/env bash
set -ev

function validate() {
  if [[ -z "$1" ]]; then
    >&2 echo "Unable to find input: '$2'."
    exit 1
  fi
}

validate "$INPUT_BUILD_FLAVOR" "buildFlavor"
validate "$INPUT_PLAY_TRACK" "playTrack"
validate "$INPUT_GOOGLE_SERVICES" "googleServices"
validate "$INPUT_PLAY_SERVICES" "playServices"
validate "$INPUT_SIGNING_KEY_STORE" "signingKeyStore"
validate "$INPUT_SIGNING_KEY_ALIAS" "signingKeyStoreAlias"
validate "$INPUT_SIGNING_KEY_STORE_PASSWORD" "signingKeyStorePassword"
validate "$INPUT_SIGNING_KEY_PASSWORD" "signingKeyPassword"
validate "$INPUT_SONAR_HEADER_VALUE" "sonarHeaderValue"
validate "$INPUT_SONAR_BASE_URL" "sonarBaseUrl"
validate "$INPUT_SONAR_URL_APPLY_CORONAVIRUS_TEST" "sonarUrlApplyCoronavirusTest"
validate "$INPUT_COMMIT_SHA" "commitSha"

CURRENT_DIR=$(pwd)

GOOGLE_SERVICES_FILE=app/google-services.json
PLAY_SERVICES_FILE=$CURRENT_DIR/build/play-services.json
SIGNING_KEY_STORE_FILE=$CURRENT_DIR/build/keystore

mkdir -p build
echo "$INPUT_GOOGLE_SERVICES" > $GOOGLE_SERVICES_FILE
echo "$INPUT_PLAY_SERVICES" > "$PLAY_SERVICES_FILE"

echo "$INPUT_SIGNING_KEY_STORE" > build/keystore.txt
base64 -d build/keystore.txt > "$SIGNING_KEY_STORE_FILE"

./gradlew "assemble$INPUT_BUILD_FLAVOR" "bundle$INPUT_BUILD_FLAVOR" "publish$INPUT_BUILD_FLAVOR" \
   --track="$INPUT_PLAY_TRACK" \
   -Pgitcommit="${INPUT_COMMIT_SHA:0:7}" \
   -Psonar.headerValue="$INPUT_SONAR_HEADER_VALUE" \
   -Psonar.baseUrl="$INPUT_SONAR_BASE_URL" \
   -Psonar.urlApplyCoronavirusTest="$INPUT_SONAR_URL_APPLY_CORONAVIRUS_TEST" \
   -Pplay-enabled \
   -PPLAY_SERVICES_PUBLISH="$PLAY_SERVICES_FILE" \
   -PSIGNING_KEY_STORE="$SIGNING_KEY_STORE_FILE" \
   -PSIGNING_KEY_ALIAS="$INPUT_SIGNING_KEY_ALIAS" \
   -PSIGNING_KEY_STORE_PASSWORD="$INPUT_SIGNING_KEY_STORE_PASSWORD" \
   -PSIGNING_KEY_PASSWORD="$INPUT_SIGNING_KEY_PASSWORD" \

# Return Build Version
BUILD_VERSION_FILE=app/build/version.txt
BUILD_VERSION=$(head -n 1 $BUILD_VERSION_FILE)
echo "::set-output name=buildVersion::$BUILD_VERSION"
