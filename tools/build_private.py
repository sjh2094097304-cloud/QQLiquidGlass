#!/usr/bin/env python3
"""Build a normally signed private debug APK without requiring the original release key."""
from pathlib import Path
import argparse
import hashlib
import os
import re
import shutil
import subprocess
import sys

from build_release import sdk_location

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--sdk', help='包含 android-35 与 build-tools 35.0.0 的 Android SDK 目录')
    args = parser.parse_args()
    environment = os.environ.copy()
    for name in ('BB_KEYSTORE', 'BB_STORE_PASSWORD', 'BB_KEY_ALIAS', 'BB_KEY_PASSWORD'):
        environment.pop(name, None)
    try:
        aapt, apksigner = sdk_location(args.sdk, environment)
        for script in ('tests/run_private_guard_check.py', 'tests/run_regression.py'):
            subprocess.run([sys.executable, str(ROOT / script)], cwd=ROOT, env=environment, check=True)
        subprocess.run(['java', '-jar', str(ROOT / 'gradle/wrapper/gradle-wrapper.jar'),
                        '--no-daemon', ':app:assembleDebug'], cwd=ROOT, env=environment, check=True)
        apk = ROOT / 'app/build/outputs/apk/debug/app-debug.apk'
        if not apk.is_file():
            raise RuntimeError('Gradle 未产出 app-debug.apk')
        badging = subprocess.run([str(aapt), 'dump', 'badging', str(apk)],
                                 capture_output=True, text=True, check=True).stdout
        for expected in ("name='com.qiutian.bianpaobubble.v36'", "versionCode='28'", "versionName='3.7'"):
            if expected not in badging:
                raise RuntimeError('实际 APK 的包名或版本不符合私人版配置')
        verification = subprocess.run(['java', '-jar', str(apksigner), 'verify', '--verbose',
                                       '--print-certs', str(apk)], capture_output=True, text=True, check=True)
        certificate = verification.stdout + verification.stderr
        if not re.search(r'certificate SHA-256 digest:\s*[0-9a-f]{64}', certificate, re.I):
            raise RuntimeError('无法读取 APK 实际签名证书，未交付安装包')
        destination = ROOT / 'build/private-deliverables'
        destination.mkdir(parents=True, exist_ok=True)
        target = destination / 'bubble-3.7-private-debug.apk'
        shutil.copy2(apk, target)
        digest = hashlib.sha256(target.read_bytes()).hexdigest()
        (destination / (target.name + '.sha256')).write_text(digest + '  ' + target.name + '\n', encoding='utf-8')
        note = ('PRIVATE DEBUG BUILD: 应用内原证书限制已取消，但 APK 安装签名已正常验证。\n'
                '不同签名不能覆盖原版；当前构建不代表 QQ 真机行为已通过验收。\n')
        (destination / 'apk-verification.txt').write_text(note + badging + '\n' + certificate, encoding='utf-8')
        print('已生成并验签：' + str(target))
    except (RuntimeError, OSError, subprocess.CalledProcessError) as error:
        print('私人构建未完成：' + str(error), file=sys.stderr)
        raise SystemExit(1)


if __name__ == '__main__':
    main()
