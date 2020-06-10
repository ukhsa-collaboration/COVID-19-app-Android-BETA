#!/usr/bin/env bash
set -ev

function validate() {
  if [[ -z "$1" ]]; then
    >&2 echo "Unable to find input: '$2'."
    exit 1
  fi
}

validate "$INPUT_GOOGLE_SERVICES" "googleServices"
validate "$INPUT_SONAR_BASE_URL" "sonarBaseUrl"
validate "$INPUT_SONAR_HEADER_VALUE" "sonarHeaderValue"
validate "$INPUT_SONAR_ANALYTICS_KEY" "sonarAnalyticsKey"
validate "$INPUT_PACTBROKER_URL" "pactbrokerUrl"

GOOGLE_SERVICES_FILE=app/google-services.json
mkdir build
echo "$INPUT_GOOGLE_SERVICES" > $GOOGLE_SERVICES_FILE

./gradlew testDebugUnitTest packageDebug packageDebugAndroidTest pactPublish \
    -Psonar.baseUrl="$INPUT_SONAR_BASE_URL" \
    -Psonar.headerValue="$INPUT_SONAR_HEADER_VALUE" \
    -Psonar.analyticsKey="$INPUT_SONAR_ANALYTICS_KEY" \
    -Ppactbroker.url="$INPUT_PACTBROKER_URL"
