#!/usr/bin/env python3
"""Build 3.7, preserving the release identity. Signing secrets never enter the source tree."""
from pathlib import Path
import argparse
import hashlib
import os
import re
import shutil
import subprocess
import sys
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
EXPECTED = 'd09188089fdf640a439181df1541a6af2276985125d18ba6db7a00892c60dc09'
SECRET_NAMES = ('BB_KEYSTORE', 'BB_STORE_PASSWORD', 'BB_KEY_ALIAS', 'BB_KEY_PASSWORD')


def imported_signing(archive, folder, environment):
    with zipfile.ZipFile(archive) as z:
        keys = [n for n in z.namelist() if n.lower().endswith('.p12')]
        notes = [n for n in z.namelist() if '密码' in n and n.endswith('.txt')]
        if len(keys) != 1 or len(notes) != 1:
            raise RuntimeError('原始压缩包中应有一份 p12 签名和一份签名密码说明')
        fields = {}
        for line in z.read(notes[0]).decode('utf-8-sig').splitlines():
            parts = re.split(r'[:：=]', line, maxsplit=1)
            if len(parts) == 2:
                fields[parts[0].strip()] = parts[1].strip()
        for key in ('别名', '密钥库密码', '密钥密码'):
            if not fields.get(key):
                raise RuntimeError('签名说明缺少字段：' + key)
        target = Path(folder) / 'release.p12'
        target.write_bytes(z.read(keys[0]))
        target.chmod(0o600)
        environment.update(BB_KEYSTORE=str(target), BB_KEY_ALIAS=fields['别名'],
                           BB_STORE_PASSWORD=fields['密钥库密码'], BB_KEY_PASSWORD=fields['密钥密码'])


def verify_key(environment):
    if any(not environment.get(key) for key in SECRET_NAMES):
        raise RuntimeError('请传入 --archive 原始源码压缩包，或设置 BB_KEYSTORE/BB_STORE_PASSWORD/BB_KEY_ALIAS/BB_KEY_PASSWORD')
    keytool = shutil.which('keytool')
    if not keytool:
        raise RuntimeError('需要 JDK 17 的 keytool')
    result = subprocess.run([keytool, '-exportcert', '-storetype', 'PKCS12',
                             '-keystore', environment['BB_KEYSTORE'], '-alias', environment['BB_KEY_ALIAS'],
                             '-storepass:env', 'BB_STORE_PASSWORD'], env=environment, capture_output=True)
    if result.returncode:
        raise RuntimeError('无法打开签名密钥，请检查别名和密码')
    if hashlib.sha256(result.stdout).hexdigest() != EXPECTED:
        raise RuntimeError('签名证书与原 3.6 安装包不符，已停止，避免生成不能覆盖升级的包')
    print('原发布证书核对通过（SHA-256）：' + EXPECTED, flush=True)


def sdk_location(explicit, environment):
    value = explicit or environment.get('ANDROID_HOME') or environment.get('ANDROID_SDK_ROOT')
    if not value and (ROOT / 'local.properties').exists():
        for line in (ROOT / 'local.properties').read_text().splitlines():
            if line.startswith('sdk.dir='):
                value = line.split('=', 1)[1].replace('\\:', ':').replace('\\\\', '\\')
                break
    if not value:
        raise RuntimeError('未找到 Android SDK；需要 platform android-35 和 build-tools 35.0.0')
    sdk = Path(value).expanduser().resolve()
    if not (sdk / 'platforms/android-35/android.jar').is_file():
        raise RuntimeError('缺少 Android SDK platform android-35')
    tools = sdk / 'build-tools/35.0.0'
    aapt = tools / ('aapt2.exe' if os.name == 'nt' else 'aapt2')
    apksigner = tools / 'lib/apksigner.jar'
    if not aapt.is_file() or not apksigner.is_file():
        raise RuntimeError('缺少 build-tools 35.0.0 的 aapt2/apksigner')
    environment['ANDROID_HOME'] = str(sdk)
    environment['ANDROID_SDK_ROOT'] = str(sdk)
    return aapt, apksigner


def build(args, environment):
    aapt, apksigner = sdk_location(args.sdk, environment)
    subprocess.run([sys.executable, str(ROOT / 'tests/run_regression.py')], cwd=ROOT, check=True)
    # The wrapper is executable on Linux/macOS; -jar also works on Windows without a .bat file.
    subprocess.run(['java', '-jar', str(ROOT / 'gradle/wrapper/gradle-wrapper.jar'),
                    '--no-daemon', ':app:assembleRelease'], cwd=ROOT, env=environment, check=True)
    name = 'app-release-unsigned.apk' if args.unsigned else 'app-release.apk'
    apk = ROOT / 'app/build/outputs/apk/release' / name
    if not apk.is_file():
        raise RuntimeError('构建未产出预期文件：' + name)
    badging = subprocess.run([str(aapt), 'dump', 'badging', str(apk)], capture_output=True, text=True, check=True).stdout
    for expected in ("name='com.qiutian.bianpaobubble.v36'", "versionCode='28'", "versionName='3.7'"):
        if expected not in badging:
            raise RuntimeError('APK 包名或版本不符合 3.7 发布配置')
    certificate = 'UNSIGNED: 仅供构建检查，不能直接安装。\n'
    if not args.unsigned:
        result = subprocess.run(['java', '-jar', str(apksigner), 'verify', '--verbose', '--print-certs', str(apk)],
                                capture_output=True, text=True, check=True)
        certificate = result.stdout
        if EXPECTED not in certificate.lower():
            raise RuntimeError('APK 发布证书核对失败')
    out = ROOT / 'build/deliverables'
    out.mkdir(parents=True, exist_ok=True)
    target = out / ('bubble-3.7-unsigned.apk' if args.unsigned else 'bubble-3.7.apk')
    shutil.copy2(apk, target)
    digest = hashlib.sha256(target.read_bytes()).hexdigest()
    (out / (target.name + '.sha256')).write_text(digest + '  ' + target.name + '\n')
    (out / 'apk-verification.txt').write_text(badging + '\n' + certificate, encoding='utf-8')
    print('生成：' + str(target), flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    group = parser.add_mutually_exclusive_group()
    group.add_argument('--archive', type=Path, help='最初上传的 气泡3.6源码.zip；包含原签名恢复资料')
    group.add_argument('--unsigned', action='store_true', help='只生成明确标记的未签名构建包')
    parser.add_argument('--sdk', help='Android SDK 目录')
    parser.add_argument('--check-signing', action='store_true', help='只校验证书，不编译、不访问网络')
    args = parser.parse_args()
    if args.unsigned and args.check_signing:
        parser.error('--unsigned 不能与 --check-signing 同时使用')
    environment = os.environ.copy()
    try:
        with tempfile.TemporaryDirectory(prefix='bubble37-signing-') as temporary:
            if args.unsigned:
                for key in SECRET_NAMES:
                    environment.pop(key, None)
            else:
                if args.archive:
                    imported_signing(args.archive, temporary, environment)
                verify_key(environment)
                if args.check_signing:
                    return
            build(args, environment)
    except (RuntimeError, OSError, subprocess.CalledProcessError, zipfile.BadZipFile) as error:
        print('构建未完成：' + str(error), file=sys.stderr)
        raise SystemExit(1)


if __name__ == '__main__':
    main()
