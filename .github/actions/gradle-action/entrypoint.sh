#!/usr/bin/env bash
set -ev

function validate() {
  if [[ -z "$1" ]]; then
    >&2 echo "Unable to find input: '$2'."
    exit 1
  fi
}

validate "$INPUT_TASKS" "tasks"
validate "$INPUT_GOOGLE_SERVICES" "googleServices"
validate "$INPUT_SONAR_BASE_URL" "sonarBaseUrl"
validate "$INPUT_SONAR_HEADER_VALUE" "sonarHeaderValue"
validate "$INPUT_PACTBROKER_URL" "pactbrokerUrl"

mkdir -p app/pacts

GOOGLE_SERVICES_FILE=app/google-services.json
mkdir -p build
echo "$INPUT_GOOGLE_SERVICES" > $GOOGLE_SERVICES_FILE

# we can have multiple tasks space separated.
# shellcheck disable=SC2086
./gradlew $INPUT_TASKS \
    -Psonar.baseUrl="$INPUT_SONAR_BASE_URL" \
    -Psonar.headerValue="$INPUT_SONAR_HEADER_VALUE" \
    -Ppactbroker.url="$INPUT_PACTBROKER_URL"
