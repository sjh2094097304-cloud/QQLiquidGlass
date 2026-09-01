# Compile-time API signatures only

These reduced stubs support `python3 tests/run_api_check.py` in environments without an Android SDK.
They **must not be included in an APK or used to replace the SDK in a release build**.

The Android signatures were selected from AOSP's
[`core/api/current.txt`](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/api/current.txt)
(Git blob `872b0ae0e3a533d9373d1670eda7efdbdadd451c`, retrieved 2026-09-01).
The exact source snapshot is recorded by its Git blob hash above.
Apache-2.0 license text is in `../third_party/LICENSE-APACHE-2.0.txt`.

The files contain signatures and stub bodies only. Unused interfaces/methods and annotations are
omitted, `final` modifiers are relaxed, and protected empty constructors allow fake superclass
bodies to compile. Xposed callback stubs and the module's fake resource ID are hand-written.
This catches application Java syntax/type errors in the selected surfaces. It cannot check Android
runtime behavior, min/target API availability, resources, R8, DEX, native QQ signatures or APK signing.
A real `assembleRelease` build and device tests remain required.
