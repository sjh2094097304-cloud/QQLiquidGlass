package com.qiutian.bianpaobubble;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import java.security.MessageDigest;
import java.util.Locale;

/** Rejects repackaged APKs signed by anyone other than the author's release key. */
public final class SignatureGuard {
    private static final String PACKAGE = "com.qiutian.bianpaobubble.v36";
    private static final String EXPECTED = "d09188089fdf640a439181df1541a6af"
            + "2276985125d18ba6db7a00892c60dc09";

    private SignatureGuard() {}

    public static void enforceInstalled(Context context) {
        if (context == null || !matches(packageInfo(context, PACKAGE, false))) {
            throw new SecurityException("百变气泡签名校验失败，安装包可能已被修改");
        }
    }

    public static void enforceArchiveIfReadable(Context context, String apkPath) {
        if (context == null || apkPath == null || apkPath.trim().isEmpty()) return;
        PackageInfo info = packageInfo(context, apkPath, true);
        if (info != null && !matches(info)) {
            throw new SecurityException("百变气泡模块签名校验失败");
        }
    }

    @SuppressWarnings("deprecation")
    private static PackageInfo packageInfo(Context context, String value, boolean archive) {
        try {
            int flags = Build.VERSION.SDK_INT >= 28
                    ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
            return archive ? context.getPackageManager().getPackageArchiveInfo(value, flags)
                    : context.getPackageManager().getPackageInfo(value, flags);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean matches(PackageInfo info) {
        if (info == null) return false;
        try {
            Signature[] signatures;
            if (Build.VERSION.SDK_INT >= 28 && info.signingInfo != null) {
                signatures = info.signingInfo.hasMultipleSigners()
                        ? info.signingInfo.getApkContentsSigners()
                        : info.signingInfo.getSigningCertificateHistory();
            } else {
                signatures = info.signatures;
            }
            if (signatures == null) return false;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Signature signature : signatures) {
                if (signature != null && EXPECTED.equals(hex(digest.digest(signature.toByteArray())))) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static String hex(byte[] value) {
        StringBuilder result = new StringBuilder(value == null ? 0 : value.length * 2);
        if (value != null) for (byte item : value) result.append(String.format(Locale.ROOT, "%02x", item & 0xff));
        return result.toString();
    }
}
