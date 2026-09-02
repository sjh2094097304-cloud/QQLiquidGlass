#!/usr/bin/env bash

set -euo pipefail

PROJECT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEST_OUT="$(mktemp -d)"

trap 'rm -rf "$TEST_OUT"' EXIT

mapfile -t STUBS < <(
    find "$PROJECT/tests/runtime-stubs" \
        -type f \
        -name '*.java' \
        -print \
        | sort
)

if [[ ${#STUBS[@]} -eq 0 ]]; then
    echo "ERROR: no runtime stub Java files found" >&2
    exit 1
fi

echo "Runtime stubs: ${#STUBS[@]}"

javac \
    -encoding UTF-8 \
    --release 11 \
    -d "$TEST_OUT" \
    "${STUBS[@]}" \
    "$PROJECT/src/io/github/liuran001/mmliquidglass/Spring.java" \
    "$PROJECT/src/io/github/liuran001/mmliquidglass/DropletDragController.java" \
    "$PROJECT/src/io/github/liuran001/mmliquidglass/QqGlassBackdrop.java" \
    "$PROJECT/src/io/github/liuran001/mmliquidglass/FeedbackLog.java" \
    "$PROJECT/tests/DropletInteractionTest.java" \
    "$PROJECT/tests/SharedBackdropTest.java"

java \
    -cp "$TEST_OUT" \
    io.github.liuran001.mmliquidglass.DropletInteractionTest

java \
    -cp "$TEST_OUT" \
    io.github.liuran001.mmliquidglass.SharedBackdropTest