#!/usr/bin/env python3
"""Check private no-op entry points against a minimal Context, not an Android runtime."""
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
GUARD = ROOT / 'app/src/main/java/com/qiutian/bianpaobubble/SignatureGuard.java'
CONTEXT = '''package android.content;
public abstract class Context {
    public Object getPackageManager() { throw new AssertionError("Must not query signing identity"); }
}
'''
CHECKS = '''import android.content.Context;
import com.qiutian.bianpaobubble.SignatureGuard;
public final class PrivateGuardChecks {
    public static void main(String[] args) {
        Context inaccessible = new Context() {};
        SignatureGuard.enforceInstalled(null);
        SignatureGuard.enforceInstalled(inaccessible);
        SignatureGuard.enforceArchiveIfReadable(null, null);
        SignatureGuard.enforceArchiveIfReadable(inaccessible, "");
        SignatureGuard.enforceArchiveIfReadable(inaccessible, "/missing/module.apk");
        SignatureGuard.enforceArchiveIfReadable(inaccessible, "/different-signer/module.apk");
        System.out.println("PASS: 6 private guard entry checks; no original-certificate lookup.");
    }
}
'''


def main():
    with tempfile.TemporaryDirectory(prefix='bubble-private-guard-check-') as temporary:
        folder = Path(temporary)
        context = folder / 'android/content/Context.java'
        context.parent.mkdir(parents=True)
        context.write_text(CONTEXT, encoding='utf-8')
        checks = folder / 'PrivateGuardChecks.java'
        checks.write_text(CHECKS, encoding='utf-8')
        subprocess.run(['java', '--module', 'jdk.compiler/com.sun.tools.javac.Main', '--release', '11',
                        '-encoding', 'UTF-8', '-d', str(folder), str(context), str(GUARD), str(checks)], check=True)
        subprocess.run(['java', '-cp', str(folder), 'PrivateGuardChecks'], check=True)
    print('Android installation signature checks are not disabled by this source change.')


if __name__ == '__main__':
    main()
