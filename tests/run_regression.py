#!/usr/bin/env python3
"""JDK-only executable regressions; Android/Binder/QQ are simulated, not device tested."""
from pathlib import Path
import subprocess
import sys

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
OUT = ROOT / 'build' / 'regression'
OUT.mkdir(parents=True, exist_ok=True)
APP = ROOT / 'app/src/main/java/com/qiutian/bianpaobubble'
sources = list((HERE / 'stubs').rglob('*.java')) + list((HERE / 'third_party').rglob('*.java'))
sources += [APP / name for name in ('AppConfig.java', 'ConfigStore.java', 'ConfigProvider.java')]
sources += [APP / 'hook' / name for name in ('HostConfig.java', 'BubbleRandomBag.java', 'ConfigCodec.java',
            'ProtoLite.java', 'Reflector.java', 'MallIdParser.java', 'WeakIdentityMap.java', 'HookStatus.java', 'HookLog.java')]
sources += [HERE / 'RegressionChecks.java', HERE / 'SyntaxCheck.java']
compiler = ['java', '--module', 'jdk.compiler/com.sun.tools.javac.Main']
subprocess.run(compiler + ['--release', '11', '-encoding', 'UTF-8', '-d', str(OUT)] + [str(p) for p in sources], check=True)
result = subprocess.run(['java', '-cp', str(OUT), 'com.qiutian.bianpaobubble.hook.RegressionChecks'],
                        text=True, capture_output=True)
print(result.stdout, end='')
if result.stderr:
    print(result.stderr, file=sys.stderr, end='')
(OUT / 'results.txt').write_text(result.stdout + result.stderr, encoding='utf-8')
if result.returncode:
    raise SystemExit(result.returncode)
subprocess.run(['java', '-cp', str(OUT), 'SyntaxCheck'] + [str(p) for p in APP.rglob('*.java')], check=True)
