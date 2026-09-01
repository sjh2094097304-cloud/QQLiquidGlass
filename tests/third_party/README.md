# Test-only Android JSON implementation

`org/json/*.java` comes from the Android Open Source Project implementation mirrored in
[LineageOS/android_libcore](https://github.com/LineageOS/android_libcore/tree/lineage-21.0/json/src/main/java/org/json).
These files retain their original Apache-2.0 headers; the license is included alongside this file.
They are used only by the JVM regressions and are not packaged into the APK, which uses Android's `org.json`.

Snapshot retrieved 2026-09-01; Git blob IDs:

| File | Blob |
| --- | --- |
| JSONObject.java | 1e8ed9c9ca6dc31ec4fb28ff339a104df918d703 |
| JSONTokener.java | b41c751133736a2da70ac68b48622796170b332a |
| JSONArray.java | df0b2437cdfd4aea89fc9831f9b41f834f922b89 |
| JSONStringer.java | ef1b47c2fe68be6f0b4f11e3ce8e9a25a783aebc |
| JSON.java | 1b32e698d010458b1797d96b85cc020b183fc3bc |
| JSONException.java | 05e1dddc9aa4666dc89175d144288f9d9aa08ce4 |
