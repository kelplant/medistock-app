#!/bin/bash
# MediStock - iOS Maestro E2E Tests Runner
# Usage: ./scripts/run_tests_ios.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
MAESTRO_BIN="${HOME}/.maestro/bin/maestro"
IOS_APP_DIR="$PROJECT_DIR/iosApp"
DERIVED_DATA_PATH="${HOME}/Library/Developer/Xcode/DerivedData"
BUNDLE_ID="com.medistock.ios"

echo "=========================================="
echo "  MediStock - iOS E2E Tests"
echo "=========================================="

# Check if Maestro is installed
if [ ! -f "$MAESTRO_BIN" ]; then
    echo "Error: Maestro not found at $MAESTRO_BIN"
    echo "Install Maestro: curl -Ls 'https://get.maestro.mobile.dev' | bash"
    exit 1
fi

# Check if iOS simulator is booted
BOOTED_DEVICE=$(xcrun simctl list devices booted | grep -E "^\s+.*\(Booted\)" | head -1)
if [ -z "$BOOTED_DEVICE" ]; then
    echo "Error: No iOS simulator is booted"
    echo "Please start an iOS simulator first:"
    echo "  open -a Simulator"
    echo "  or: xcrun simctl boot 'iPhone 16 Pro'"
    exit 1
fi

DEVICE_NAME=$(echo "$BOOTED_DEVICE" | sed 's/^[[:space:]]*//' | sed 's/ (.*$//')
echo "iOS Simulator detected: $DEVICE_NAME"
echo ""

# Check if app is installed
if ! xcrun simctl listapps booted 2>/dev/null | grep -q "$BUNDLE_ID"; then
    echo "MediStock app not installed. Building and installing..."

    # Build the app
    cd "$IOS_APP_DIR"
    xcodebuild -project iosApp.xcodeproj \
        -scheme iosApp \
        -destination "platform=iOS Simulator,name=$DEVICE_NAME" \
        -derivedDataPath "$DERIVED_DATA_PATH" \
        build 2>&1 | tail -5

    if [ $? -ne 0 ]; then
        echo "Error: Failed to build iOS app"
        exit 1
    fi

    # Install the app
    APP_PATH="$DERIVED_DATA_PATH/Build/Products/Debug-iphonesimulator/iosApp.app"
    if [ -d "$APP_PATH" ]; then
        xcrun simctl install booted "$APP_PATH"
        echo "App installed successfully"
    else
        echo "Error: Built app not found at $APP_PATH"
        exit 1
    fi
fi

echo ""
echo "Running Maestro tests..."
echo "=========================================="

# Run tests with iOS platform flag
cd "$PROJECT_DIR"
"$MAESTRO_BIN" -p ios test --no-parallel .maestro/ios/

EXIT_CODE=$?

echo ""
echo "=========================================="
if [ $EXIT_CODE -eq 0 ]; then
    echo "All iOS tests PASSED"
else
    echo "Some iOS tests FAILED"
fi
echo "=========================================="

exit $EXIT_CODE
