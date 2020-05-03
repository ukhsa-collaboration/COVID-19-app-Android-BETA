#!/bin/bash

set -e

if [[ ! $# -eq 2 ]]; then
  echo "Usage:  $0 track<test|alpha>  ref<git sha|ref>"
  exit 1
fi

track=$1
ref=$2

if [[ -z "$track" ]]; then
  echo "No 'track' is provided"
  exit 1
fi

if [[ "$track" != "test" && "$track" != "alpha" ]]; then
  echo "Invalid track. Select 'test' or 'alpha'"
  exit 1
fi

if [[ -z "$ref" ]]; then
  echo "No 'ref' is provided" 
  exit 1
fi

if [[ -z "$GITHUB_USER_TOKEN" ]]; then
  echo "GITHUB_USER_TOKEN is not set in env." 
  exit 1
fi

echo
echo "You are about to trigger a new '$track' release using '$ref' sha/ref"
echo
read -p "Are you sure? " -n 1 -r
echo

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
   exit 1
fi

url="https://api.github.com/repos/nhsx/sonar-colocate-android/dispatches"
contentType="Content-Type: application/json"
payload="{\"event_type\": \"publish-$track\", \"client_payload\": {\"ref\": \"$ref\"}}"

echo
echo "Dispatching event: $payload"
echo

curl -i -u "$GITHUB_USER_TOKEN" -H "$contentType" -d "$payload" "$url"

