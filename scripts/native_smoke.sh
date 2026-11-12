#!/usr/bin/env bash
set -euo pipefail
image=$1
cleanup() {
  docker logs t2g-native-sanity > native-sanity.log 2>&1 || true
  docker rm -f t2g-native-sanity t2g-release-firestore >/dev/null 2>&1 || true
}
trap cleanup EXIT
# Both the native runtime and its data stay on this disposable GitHub runner.
docker run -d --name t2g-release-firestore --network host \
  gcr.io/google.com/cloudsdktool/google-cloud-cli:583.0.0-emulators \
  gcloud emulators firestore start --host-port=127.0.0.1:18080 --project=demo-time2gather-release --quiet
for attempt in $(seq 1 60); do
  if curl -fsS http://127.0.0.1:18080/ >/dev/null; then break; fi
  sleep 1
done
curl -fsS http://127.0.0.1:18080/ >/dev/null
docker run -d --name t2g-native-sanity --network host \
  -e PORT=18081 -e GCP_PROJECT_ID=demo-time2gather-release \
  -e FIRESTORE_EMULATOR_HOST=127.0.0.1:18080 \
  -e JWT_SECRET=local-release-sanity-secret-at-least-256-bits-long \
  "$image"
for attempt in $(seq 1 60); do
  if curl -fsS http://127.0.0.1:18081/health >/dev/null; then break; fi
  sleep 1
done
curl -fsS http://127.0.0.1:18081/health >/dev/null
SANITY_API_BASE=http://127.0.0.1:18081/api node scripts/sanity-api.mjs
