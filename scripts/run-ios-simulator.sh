#!/bin/bash
#
# Build, install and launch the iOS app on a running iOS Simulator.
#
# Exists because the Kotlin Multiplatform plugin's "Targets" dropdown in
# Android Studio cannot enumerate simulators on this setup (SimulatorsRegistry
# NPEs while parsing CoreSimulator device-type profiles). This script does the
# same work the IDE would do, so it can be wired to a Run configuration.
#
# Usage:
#   ./scripts/run-ios-simulator.sh                 # use the booted simulator
#   ./scripts/run-ios-simulator.sh "iPhone 17"     # use a named simulator
#
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
XCODEPROJ="$PROJECT_ROOT/app/iosApp/iosApp.xcodeproj"
SCHEME="iosApp"
CONFIGURATION="Debug"
DERIVED_DATA="$PROJECT_ROOT/build/ios-derived-data"
FALLBACK_DEVICE="iPhone 17"

# --- Resolve the target simulator UDID -------------------------------------
requested="${1:-}"

if [ -n "$requested" ]; then
    udid="$(xcrun simctl list devices available -j \
        | python3 -c '
import json, sys
name = sys.argv[1]
data = json.load(sys.stdin)["devices"]
for runtime, devices in data.items():
    if "iOS" not in runtime:
        continue
    for d in devices:
        if d["name"] == name:
            print(d["udid"])
            sys.exit(0)
' "$requested")"
    if [ -z "$udid" ]; then
        echo "error: no available iOS simulator named '$requested'" >&2
        echo "hint: xcrun simctl list devices available" >&2
        exit 1
    fi
else
    # Prefer an already-booted simulator so we deploy into what you're looking at.
    udid="$(xcrun simctl list devices booted -j \
        | python3 -c '
import json, sys
data = json.load(sys.stdin)["devices"]
for runtime, devices in data.items():
    if "iOS" not in runtime:
        continue
    for d in devices:
        print(d["udid"])
        sys.exit(0)
')"
fi

if [ -z "$udid" ]; then
    echo "No simulator booted. Booting '$FALLBACK_DEVICE'..."
    udid="$(xcrun simctl list devices available -j \
        | python3 -c '
import json, sys
name = sys.argv[1]
data = json.load(sys.stdin)["devices"]
for runtime, devices in data.items():
    if "iOS" not in runtime:
        continue
    for d in devices:
        if d["name"] == name:
            print(d["udid"])
            sys.exit(0)
' "$FALLBACK_DEVICE")"
    if [ -z "$udid" ]; then
        echo "error: fallback simulator '$FALLBACK_DEVICE' not found" >&2
        exit 1
    fi
fi

device_name="$(xcrun simctl list devices -j \
    | python3 -c '
import json, sys
udid = sys.argv[1]
data = json.load(sys.stdin)["devices"]
for runtime, devices in data.items():
    for d in devices:
        if d["udid"] == udid:
            print(d["name"])
            sys.exit(0)
' "$udid")"

echo "==> Simulator: ${device_name:-unknown} ($udid)"

# Boot if needed, then wait until it is usable.
state="$(xcrun simctl list devices -j \
    | python3 -c '
import json, sys
udid = sys.argv[1]
data = json.load(sys.stdin)["devices"]
for runtime, devices in data.items():
    for d in devices:
        if d["udid"] == udid:
            print(d["state"])
            sys.exit(0)
' "$udid")"

if [ "$state" != "Booted" ]; then
    xcrun simctl boot "$udid"
fi
xcrun simctl bootstatus "$udid" -b >/dev/null

# Bring the simulator window forward if the GUI app is installed.
simulator_app="$(xcode-select -p)/Applications/Simulator.app"
if [ -d "$simulator_app" ]; then
    open -a "$simulator_app" || true
fi

# --- Build -----------------------------------------------------------------
# The Xcode target's "Compile Kotlin Framework" phase invokes
# :app:shared:embedAndSignAppleFrameworkForXcode, so Kotlin changes are picked
# up here automatically.
echo "==> Building $SCHEME ($CONFIGURATION) for simulator"
xcodebuild \
    -project "$XCODEPROJ" \
    -scheme "$SCHEME" \
    -configuration "$CONFIGURATION" \
    -destination "id=$udid" \
    -derivedDataPath "$DERIVED_DATA" \
    build

app_path="$DERIVED_DATA/Build/Products/$CONFIGURATION-iphonesimulator/Kmp.app"
if [ ! -d "$app_path" ]; then
    # Fall back to whatever .app the build produced.
    app_path="$(find "$DERIVED_DATA/Build/Products/$CONFIGURATION-iphonesimulator" \
        -maxdepth 1 -name '*.app' -print -quit 2>/dev/null || true)"
fi
if [ -z "$app_path" ] || [ ! -d "$app_path" ]; then
    echo "error: could not locate built .app under $DERIVED_DATA" >&2
    exit 1
fi

bundle_id="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "$app_path/Info.plist")"

# --- Install and launch ----------------------------------------------------
echo "==> Installing $(basename "$app_path") ($bundle_id)"
xcrun simctl install "$udid" "$app_path"

echo "==> Launching $bundle_id"
xcrun simctl launch --console-pty "$udid" "$bundle_id"
