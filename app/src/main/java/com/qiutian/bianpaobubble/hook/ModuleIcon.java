package com.qiutian.bianpaobubble.hook;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Loads the module avatar without depending on FPA resource-hook support. */
final class ModuleIcon {
    private static final String MODULE_PACKAGE = "com.qiutian.bianpaobubble.v36";
    private static final String ICON_ASSET = "assets/app_icon.jpg";
    private static volatile Bitmap cachedBitmap;
    private static volatile long lastAttemptAt;
    private static volatile String lastError = "尚未加载";
    private static volatile String moduleApkPath;

    private ModuleIcon() {}

    static void setModuleApkPath(String path) {
        if (path == null || path.trim().isEmpty()) return;
        moduleApkPath = path;
        lastAttemptAt = 0L;
        cachedBitmap = null;
    }

    static Drawable load(Context context) {
        if (context == null) return null;
        Bitmap bitmap = loadAssetBitmap(context);
        if (bitmap != null) return new BitmapDrawable(context.getResources(), bitmap);
        try {
            Context module = context.createPackageContext(MODULE_PACKAGE,
                    Context.CONTEXT_IGNORE_SECURITY | Context.CONTEXT_INCLUDE_CODE);
            int icon = module.getResources().getIdentifier("app_icon", "drawable", MODULE_PACKAGE);
            if (icon != 0) return module.getResources().getDrawable(icon, module.getTheme());
        } catch (Throwable error) {
            lastError = "资源回退失败: " + error.getClass().getSimpleName();
        }
        return null;
    }

    static String diagnostic() {
        return cachedBitmap != null ? "头像加载正常" : "头像加载失败（" + lastError + "）";
    }

    private static Bitmap loadAssetBitmap(Context context) {
        if (cachedBitmap != null) return cachedBitmap;
        long now = System.currentTimeMillis();
        if (now - lastAttemptAt < 1500L) return null;
        synchronized (ModuleIcon.class) {
            if (cachedBitmap != null) return cachedBitmap;
            lastAttemptAt = now;
            String[] paths = new String[]{moduleApkPath, moduleSourceDir(context)};
            for (String path : paths) {
                Bitmap decoded = decodeFromApk(path);
                if (decoded != null) {
                    cachedBitmap = decoded;
                    lastError = "";
                    return decoded;
                }
            }
            try {
                ClassLoader loader = ModuleIcon.class.getClassLoader();
                InputStream input = loader == null ? null : loader.getResourceAsStream(ICON_ASSET);
                if (input == null) input = ModuleIcon.class.getResourceAsStream("/" + ICON_ASSET);
                if (input != null) {
                    try {
                        cachedBitmap = BitmapFactory.decodeStream(input);
                    } finally {
                        input.close();
                    }
                }
            } catch (Throwable error) {
                lastError = "类加载器解码失败: " + error.getClass().getSimpleName();
            }
            if (cachedBitmap == null && lastError.isEmpty()) lastError = "APK 内未找到 app_icon.jpg";
            return cachedBitmap;
        }
    }

    private static String moduleSourceDir(Context context) {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(MODULE_PACKAGE, 0);
            return info == null ? null : info.sourceDir;
        } catch (Throwable error) {
            lastError = "无法定位模块 APK: " + error.getClass().getSimpleName();
            return null;
        }
    }

    private static Bitmap decodeFromApk(String path) {
        if (path == null || path.trim().isEmpty()) return null;
        try (ZipFile apk = new ZipFile(path)) {
            ZipEntry entry = apk.getEntry(ICON_ASSET);
            if (entry == null) return null;
            try (InputStream input = apk.getInputStream(entry)) {
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                if (bitmap == null) lastError = "头像图片解码为空";
                return bitmap;
            }
        } catch (Throwable error) {
            lastError = "APK 解码失败: " + error.getClass().getSimpleName();
            return null;
        }
    }
}
