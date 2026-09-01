#!/usr/bin/env python3
"""Compile all app Java against reduced AOSP signatures. This is NOT an APK build."""
from pathlib import Path
import subprocess

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
OUT = ROOT / 'build/api-check'
OUT.mkdir(parents=True, exist_ok=True)
sources = list((HERE / 'api_stubs').rglob('*.java'))
sources += list((ROOT / 'app/src/main/java').rglob('*.java'))
sources += list((HERE / 'third_party').rglob('*.java'))
for name in ['android/annotation/SystemApi.java', 'android/compat/annotation/UnsupportedAppUsage.java',
             'libcore/util/NonNull.java', 'libcore/util/Nullable.java']:
    sources.append(HERE / 'stubs' / name)
subprocess.run(['java', '--module', 'jdk.compiler/com.sun.tools.javac.Main', '--release', '11',
                '-encoding', 'UTF-8', '-d', str(OUT)] + [str(p) for p in sources], check=True)
print('PASS: all application Java compiled against reduced API signatures (not SDK/DEX/APK verification).')
