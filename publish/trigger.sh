#!/bin/bash
set -e

function main() {

track=$1
ref=$2

validate "$track" "track"
validate "$ref" "ref"
validate "$GITHUB_USER_TOKEN" "GITHUB_USER_TOKEN"	


if [[ "$track" != "test" && "$track" != "alpha" ]]; then
  >&2 echo "Invalid track. Select 'test' or 'alpha'"
  exit 1
fi


echo ""
echo "You are about to trigger a new '$track' release using '$ref' sha/ref"
echo ""
read -p "Are you sure? " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
   exit 1
fi

url="https://api.github.com/repos/nhsx/sonar-colocate-android/dispatches"
contentType="Content-Type: application/json"
payload="{\"event_type\": \"publish-$track\", \"client_payload\": {\"ref\": \"$ref\"}}"

echo ""
echo "Dispatching event: $payload"
echo ""

curl -i -u "$GITHUB_USER_TOKEN" -H "$contentType" -d "$payload" "$url"

}

function validate() {
  if [[ -z "$1" ]]; then
    >&2 echo "Unable to find the '$2'." 
    
    usage
    
    exit 1
  fi	  
}

function usage() {
  echo ""
  echo "Usage: $0 track<test|alpha>  ref<git sha|ref>"
  echo ""
}

main "$1" "$2"
