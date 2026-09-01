package com.qiutian.bianpaobubble;

import android.content.Context;

/**
 * Private-use variant: no application-level pinning to the original signer.
 * Android still verifies APK signatures and enforces signing identity on updates.
 * Keep both entry points so launcher and Hook call sites remain unchanged.
 */
public final class SignatureGuard {
    private SignatureGuard() {}

    public static void enforceInstalled(Context context) {
        // Intentionally disabled for the owner's private builds.
    }

    public static void enforceArchiveIfReadable(Context context, String apkPath) {
        // Intentionally disabled, including when an isolated framework cannot read the APK.
    }
}
