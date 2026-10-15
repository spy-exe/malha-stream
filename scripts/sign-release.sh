#!/bin/bash
# Authorized operator runs this as root INSIDE the dedicated CT.
set -euo pipefail
[[ $(hostname) == malha-stream-build && $EUID == 0 ]] || { echo 'Run as root in the dedicated CT' >&2; exit 1; }
malha_sdk=/home/malha/android-sdk/build-tools/36.0.0
malha_project=/home/malha/malha-stream
malha_signing=/root/.malha-signing
test -f "$malha_signing/release.jks"
test -f "$malha_signing/password.txt"
install -d -o malha -g malha "$malha_project/artifacts"
# PKCS12 uses the store password for the key; a second file: read would consume
# the next password line. The signer tries the store password automatically.
java -jar "$malha_sdk/lib/apksigner.jar" sign --ks "$malha_signing/release.jks" \
    --ks-key-alias malha-stream --ks-pass "file:$malha_signing/password.txt" \
    --out "$malha_project/artifacts/malha-release.apk" \
    "$malha_project/app/build/outputs/apk/nonRoot/release/app-nonRoot-release-unsigned.apk"
"$malha_sdk/apksigner" verify --verbose --print-certs "$malha_project/artifacts/malha-release.apk"
"$malha_sdk/zipalign" -c -P 16 4 "$malha_project/artifacts/malha-release.apk"
cp "$malha_project/app/build/outputs/apk/nonRoot/debug/app-nonRoot-debug.apk" "$malha_project/artifacts/malha-debug.apk"
cp "$malha_project/app/build/outputs/apk/androidTest/nonRoot/debug/app-nonRoot-debug-androidTest.apk" "$malha_project/artifacts/malha-android-test.apk"
chmod 644 "$malha_project/artifacts/"*.apk
cd "$malha_project/artifacts"
sha256sum *.apk > SHA256SUMS
runuser -u malha -- git -C "$malha_project" rev-parse HEAD > SOURCE_COMMIT
