#!/usr/bin/env bash
set -ev

function validate() {
  if [[ -z "$1" ]]; then
    >&2 echo "Unable to find input: '$2'."
    exit 1
  fi
}

validate "$INPUT_GOOGLE_SERVICES" "Google Services"
validate "$INPUT_SONAR_HEADER_VALUE" "Sonar Header Value"
validate "$INPUT_SONAR_ANALYTICS_KEY" "Sonar Analytics Key"

GOOGLE_SERVICES_FILE=app/google-services.json
mkdir build
echo "$INPUT_GOOGLE_SERVICES" > $GOOGLE_SERVICES_FILE

./gradlew build packageDebugAndroidTest \
    -Psonar.headerValue="$INPUT_SONAR_HEADER_VALUE" \
    -Psonar.analyticsKey="$INPUT_SONAR_ANALYTICS_KEY"
