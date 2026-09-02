#!/usr/bin/env bash
set -euo pipefail
PROJECT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEST_OUT="$(mktemp -d)"
javac -encoding UTF-8 --release 11 -d "$TEST_OUT" \
    "$PROJECT/src/io/github/liuran001/mmliquidglass/DockGeometry.java" \
    "$PROJECT/src/io/github/liuran001/mmliquidglass/UiWorkGate.java" \
    "$PROJECT/src/io/github/liuran001/mmliquidglass/DockOptions.java" \
    "$PROJECT/src/io/github/liuran001/mmliquidglass/FeedbackLog.java" \
    "$PROJECT/src/io/github/liuran001/mmliquidglass/RgbColor.java" \
    "$PROJECT/src/io/github/liuran001/mmliquidglass/PreviewGeometry.java" \
    "$PROJECT/tests/DockGeometryTest.java" "$PROJECT/tests/UiWorkGateTest.java" \
    "$PROJECT/tests/DockOptionsTest.java" "$PROJECT/tests/FeedbackLogTest.java" \
    "$PROJECT/tests/SourceSafetyTest.java" "$PROJECT/tests/ColorPreviewTest.java"
java -cp "$TEST_OUT" io.github.liuran001.mmliquidglass.DockGeometryTest
java -cp "$TEST_OUT" io.github.liuran001.mmliquidglass.UiWorkGateTest
java -cp "$TEST_OUT" io.github.liuran001.mmliquidglass.DockOptionsTest
java -cp "$TEST_OUT" io.github.liuran001.mmliquidglass.FeedbackLogTest
java -cp "$TEST_OUT" io.github.liuran001.mmliquidglass.SourceSafetyTest "$PROJECT"
java -cp "$TEST_OUT" io.github.liuran001.mmliquidglass.ColorPreviewTest
bash "$PROJECT/tests/runtime.sh"
bash -n "$PROJECT/build.sh" "$PROJECT/setup-tools.sh"
echo 'PASS: build script syntax'
