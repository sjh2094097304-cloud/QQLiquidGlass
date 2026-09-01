# Android manifest and Xposed assets refer to these entry points by exact class name.
-keep public class com.qiutian.bianpaobubble.MainActivity { public <init>(); }
-keep public class com.qiutian.bianpaobubble.ConfigProvider { public <init>(); }
-keep public class com.qiutian.bianpaobubble.hook.BubbleXposedInit {
    public <init>();
    public void initZygote(de.robv.android.xposed.IXposedHookZygoteInit$StartupParam);
    public void handleLoadPackage(de.robv.android.xposed.callbacks.XC_LoadPackage$LoadPackageParam);
}

# Xposed/FPA and QQ invoke/discover this package through entry metadata,
# reflection, anonymous callbacks and runtime class scans. The previous 3.6
# build retained only the entry shell and shrank 84 dex classes to 12, which
# made the module appear installed but prevented injection.
-keep class com.qiutian.bianpaobubble.hook.** { *; }
-keep interface com.qiutian.bianpaobubble.hook.** { *; }
-keep class com.qiutian.bianpaobubble.AppConfig { *; }
-keep class com.qiutian.bianpaobubble.SignatureGuard { *; }

# Preserve metadata used by Android callbacks, anonymous Xposed hooks and reflection.
-keepattributes Exceptions,InnerClasses,EnclosingMethod,Signature,*Annotation*
-allowaccessmodification
-dontusemixedcaseclassnames
-dontwarn de.robv.android.xposed.**
-dontwarn sun.misc.Unsafe

# Release hardening: strip ordinary logging calls while keeping diagnostic provider logs.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
