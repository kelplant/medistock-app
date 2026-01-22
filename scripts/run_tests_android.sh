#!/bin/bash
# MediStock - Android Maestro E2E Tests Runner
# Usage: ./scripts/run_tests_android.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
MAESTRO_BIN="${HOME}/.maestro/bin/maestro"

# Find Android SDK
if [ -z "$ANDROID_HOME" ]; then
    if [ -d "$HOME/Library/Android/sdk" ]; then
        ANDROID_HOME="$HOME/Library/Android/sdk"
    elif [ -d "$HOME/Android/Sdk" ]; then
        ANDROID_HOME="$HOME/Android/Sdk"
    fi
fi
ADB="$ANDROID_HOME/platform-tools/adb"

echo "=========================================="
echo "  MediStock - Android E2E Tests"
echo "=========================================="

# Check if Maestro is installed
if [ ! -f "$MAESTRO_BIN" ]; then
    echo "Error: Maestro not found at $MAESTRO_BIN"
    echo "Install Maestro: curl -Ls 'https://get.maestro.mobile.dev' | bash"
    exit 1
fi

# Check if adb exists
if [ ! -f "$ADB" ]; then
    echo "Error: adb not found at $ADB"
    echo "Please set ANDROID_HOME environment variable"
    exit 1
fi

# Check if Android emulator is running
if ! "$ADB" devices | grep -q "emulator"; then
    echo "Error: No Android emulator detected"
    echo "Please start an Android emulator first"
    exit 1
fi

echo "Android emulator detected"
echo ""

# Check if app is installed
if ! "$ADB" shell pm list packages | grep -q "com.medistock"; then
    echo "Warning: MediStock app not installed on emulator"
    echo "Building and installing..."

    cd "$PROJECT_DIR"
    ./gradlew :app:installDebug

    if [ $? -ne 0 ]; then
        echo "Error: Failed to build and install app"
        exit 1
    fi
    echo "App installed successfully"
fi

echo ""
echo "Running Maestro tests..."
echo "=========================================="

# Run tests
cd "$PROJECT_DIR"
"$MAESTRO_BIN" test .maestro/android/

EXIT_CODE=$?

echo ""
echo "=========================================="
if [ $EXIT_CODE -eq 0 ]; then
    echo "All Android tests PASSED"
else
    echo "Some Android tests FAILED"
fi
echo "=========================================="

exit $EXIT_CODE
