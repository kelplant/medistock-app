#!/bin/bash
# MediStock - Run All Maestro E2E Tests (Android + iOS)
# Usage: ./scripts/run_tests_all.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=========================================="
echo "  MediStock - All E2E Tests"
echo "=========================================="
echo ""

ANDROID_RESULT=0
IOS_RESULT=0

# Run Android tests
echo ">>> Running Android tests..."
echo ""
"$SCRIPT_DIR/run_tests_android.sh"
ANDROID_RESULT=$?

echo ""
echo ""

# Run iOS tests
echo ">>> Running iOS tests..."
echo ""
"$SCRIPT_DIR/run_tests_ios.sh"
IOS_RESULT=$?

echo ""
echo "=========================================="
echo "  SUMMARY"
echo "=========================================="
if [ $ANDROID_RESULT -eq 0 ]; then
    echo "  Android: PASSED"
else
    echo "  Android: FAILED"
fi

if [ $IOS_RESULT -eq 0 ]; then
    echo "  iOS:     PASSED"
else
    echo "  iOS:     FAILED"
fi
echo "=========================================="

if [ $ANDROID_RESULT -eq 0 ] && [ $IOS_RESULT -eq 0 ]; then
    echo "All tests PASSED"
    exit 0
else
    echo "Some tests FAILED"
    exit 1
fi
