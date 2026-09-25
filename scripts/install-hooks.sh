#!/usr/bin/env bash
#
# Points git at the hooks committed in .githooks/ so they are versioned and shared
# rather than living untracked in each clone's .git/hooks.
#
# Run once after cloning:
#   scripts/install-hooks.sh

set -euo pipefail

cd "$(dirname "$0")/.."

chmod +x .githooks/* scripts/*.sh
git config core.hooksPath .githooks

echo "Hooks installed. core.hooksPath -> .githooks"
echo "Pre-commit now runs ./gradlew qualityCheck."
echo
echo "To skip once (use sparingly): git commit --no-verify"
