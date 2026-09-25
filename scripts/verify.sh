#!/usr/bin/env bash
#
# The one command that means "safe to commit".
#
# Wraps ./gradlew verify so that CI, the git hook, humans and AI agents all run
# exactly the same thing. If you find yourself running a different subset to decide
# whether a change is good, that subset belongs in here instead.
#
# Usage:
#   scripts/verify.sh            # everything
#   scripts/verify.sh --quick    # formatting, detekt, architecture, conventions only

set -euo pipefail

cd "$(dirname "$0")/.."

if [[ "${1:-}" == "--quick" ]]; then
  echo "==> qualityCheck (no tests)"
  exec ./gradlew qualityCheck
fi

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "note: not running on macOS, so Apple targets are skipped."
  echo "      CI covers them in the macos job."
fi

echo "==> verify: format, static analysis, architecture, coverage, tests on all targets"
./gradlew verify

echo
echo "==> coverage"
./gradlew koverLog
