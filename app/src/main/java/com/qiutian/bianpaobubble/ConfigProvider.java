package com.qiutian.bianpaobubble;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;

/** IPC facade; storage rules are shared with the isolated-host fallback. */
public final class ConfigProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }

    @Override public Bundle call(String method, String arg, Bundle extras) {
        if (!isAllowedCaller()) {
            Bundle denied = new Bundle();
            denied.putBoolean("_ok", false);
            denied.putBoolean("_authDenied", true);
            denied.putInt("_callingUid", Binder.getCallingUid());
            return denied;
        }
        Bundle result = ConfigStore.call(AppConfig.prefs(getContext()), method, extras, false);
        result.putString("_configChannel", "模块 Provider");
        if ("healthCheck".equals(method)) {
            result.putBoolean("_callerAuthorized", true);
            result.putInt("_callingUid", Binder.getCallingUid());
            result.putString("_providerPackage", getContext().getPackageName());
        }
        return result;
    }

    private boolean isAllowedCaller() {
        int uid = Binder.getCallingUid();
        if (uid == android.os.Process.myUid()) return true;
        try {
            String[] packages = getContext().getPackageManager().getPackagesForUid(uid);
            if (packages != null) for (String name : packages) {
                if ("com.tencent.mobileqq".equals(name) || "fun.fpa".equals(name)) return true;
            }
        } catch (Throwable ignored) {}
        // Process names are caller-controlled and cannot authorize another UID.
        // Other isolated frameworks use the QQ-local store instead.
        return false;
    }

    @Override public Cursor query(Uri uri, String[] p, String s, String[] a, String o) { return null; }
    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String selection, String[] args) { return 0; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}
