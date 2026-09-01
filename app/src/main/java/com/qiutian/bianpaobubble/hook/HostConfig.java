package com.qiutian.bianpaobubble.hook;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import com.qiutian.bianpaobubble.AppConfig;
import com.qiutian.bianpaobubble.ConfigStore;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Durable local state with asynchronous IPC and bounded recovery for isolated loaders. */
final class HostConfig {
    private static final long CACHE_MAX_AGE_MS = 20_000L;
    private static final Uri URI = Uri.parse("content://" + AppConfig.AUTHORITY);
    private static final String HOST_CONFIG_PREFS = "bianbian_bubble_host_config_v36";
    private static final String HOST_NOTICE_PREFS = "bianbian_bubble_host_notice";
    private static final Object IO = new Object();
    private static final Object STATE = new Object();
    private static volatile Bundle cache = Bundle.EMPTY;
    private static volatile long sourceReadAt = -1L;
    private static volatile long nextProviderProbe;
    private static volatile long lastHookReportTime;
    private static volatile String providerError = "";
    private static volatile boolean providerAvailable;
    private static int providerFailures;
    private static final AtomicBoolean REFRESHING = new AtomicBoolean();
    private static final AtomicLong GENERATION = new AtomicLong();
    private static final ThreadLocal<Long> WRITE_GENERATION = new ThreadLocal<>();
    private static final AtomicReference<ScheduledFuture<?>> APPLIED_WRITE = new AtomicReference<>();
    private static final ScheduledThreadPoolExecutor WRITES = new ScheduledThreadPoolExecutor(1, task -> {
        Thread t = new Thread(task, "BianBianBubble-Config"); t.setDaemon(true); return t;
    });
    static { WRITES.setRemoveOnCancelPolicy(true); }
    private HostConfig() {}

    private static Context app(Context context) {
        if (context == null) return null;
        Context result = context.getApplicationContext();
        return result == null ? context : result;
    }
    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(HOST_CONFIG_PREFS, Context.MODE_PRIVATE);
    }
    static boolean success(Bundle value) {
        return value != null && value.getBoolean("_ok", false) && !value.getBoolean("_authDenied", false)
                && !value.containsKey("_error") && value.getIntArray("pool") != null;
    }
    private static boolean configuration(Bundle value) {
        return success(value) && value.containsKey("masterEnabled") && value.containsKey("randomEnabled")
                && value.containsKey("lockedEnabled") && value.containsKey("lockedId");
    }
    private static Bundle copy(Bundle value) {
        Bundle result = new Bundle(value);
        int[] pool = value.getIntArray("pool");
        if (pool != null) result.putIntArray("pool", pool.clone());
        return result;
    }
    private static void accept(Bundle value, long generation, boolean sourceRead) {
        if (!configuration(value)) return;
        synchronized (STATE) {
            if (generation != GENERATION.get()) return;
            Bundle next = AppConfig.normalize(value);
            // A configuration refresh must not rewind a send that occurred during IPC.
            if (next.getBoolean("masterEnabled", false) && cache.containsKey("lastAppliedId")) {
                next.putInt("lastAppliedId", cache.getInt("lastAppliedId", 0));
            }
            cache = next;
            if (sourceRead) sourceReadAt = SystemClock.elapsedRealtime();
        }
    }

    /** Hot-path reads never synchronously enter the module provider. */
    static Bundle get(Context context) {
        Context host = app(context);
        if (!configuration(cache) && host != null) {
            long generation = GENERATION.get();
            // Do not wait for the IPC lock even on the first outgoing message.
            Bundle local = ConfigStore.call(prefs(host), "getConfig", null, true);
            synchronized (STATE) {
                if (!configuration(cache)) accept(channel(local), generation, false);
            }
        }
        if (host != null && (sourceReadAt < 0 || SystemClock.elapsedRealtime() - sourceReadAt >= CACHE_MAX_AGE_MS)) requestRefresh(host);
        return configuration(cache) ? copy(cache) : Bundle.EMPTY;
    }
    static Bundle getForSend(Context context) { return get(context); }

    private static void requestRefresh(Context context) {
        if (!REFRESHING.compareAndSet(false, true)) return;
        runAsync(() -> { try { refresh(context); } finally { REFRESHING.set(false); } });
    }
    static Bundle refresh(Context context) {
        Context host = app(context);
        if (host == null) return Bundle.EMPTY;
        long generation = operationGeneration();
        synchronized (IO) {
            Bundle local = ConfigStore.call(prefs(host), "getConfig", null, true);
            Bundle latest = synchronize(host, local, false);
            accept(latest, generation, true);
            if (!configuration(latest)) sourceReadAt = SystemClock.elapsedRealtime();
            return configuration(cache) ? copy(cache) : latest;
        }
    }

    private static Bundle synchronize(Context host, Bundle local, boolean force) {
        if (!configuration(local)) return local;
        if (!force && SystemClock.elapsedRealtime() < nextProviderProbe) return channel(local);
        try {
            boolean dirty = local.getBoolean("_pendingSync", false);
            // Replay durable local changes before accepting any older remote snapshot.
            Bundle remote = host.getContentResolver().call(URI, dirty ? "saveSettings" : "getConfig", null, dirty ? local : null);
            if (!configuration(remote)) throw new IllegalStateException(remote == null ? "Provider 无返回" : remote.getString("_error", "Provider 配置不可用"));
            if (!dirty && local.getLong("settingsRevision", 0L) > remote.getLong("settingsRevision", 0L)) {
                remote = host.getContentResolver().call(URI, "saveSettings", null, local);
                if (!configuration(remote)) throw new IllegalStateException("较新的本地配置尚未同步");
                dirty = true;
            }
            if (!ConfigStore.mirror(prefs(host), remote, dirty, local.getLong("settingsRevision", 0L))) {
                throw new IllegalStateException("本地配置已更新或镜像写入失败，待同步设置已保留");
            }
            providerAvailable = true; providerError = ""; providerFailures = 0; nextProviderProbe = 0L;
            return channel(ConfigStore.call(prefs(host), "getConfig", null, true));
        } catch (Throwable error) {
            providerAvailable = false;
            providerError = error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage());
            providerFailures = Math.min(4, providerFailures + 1);
            nextProviderProbe = SystemClock.elapsedRealtime() + Math.min(300_000L, 30_000L << (providerFailures - 1));
            Bundle latest = ConfigStore.call(prefs(host), "getConfig", null, true);
            return channel(configuration(latest) ? latest : local);
        }
    }
    private static Bundle channel(Bundle value) {
        Bundle result = copy(value);
        result.putBoolean("_configFallback", !providerAvailable);
        result.putString("_configChannel", providerAvailable ? "QQ 本地存储（已同步 Provider）" : "QQ 本地兼容存储");
        if (!providerError.isEmpty()) result.putString("_providerError", providerError);
        return result;
    }
    static long generation() { return GENERATION.get(); }
    static long reserveWrite() { synchronized (STATE) { return GENERATION.incrementAndGet(); } }

    private static long operationGeneration() {
        Long captured = WRITE_GENERATION.get();
        return captured == null ? GENERATION.get() : captured;
    }
    private static Bundle mutate(Context context, String method, Bundle extras) {
        Context host = app(context);
        if (host == null) return Bundle.EMPTY;
        long generation = operationGeneration();
        synchronized (IO) {
            Bundle result = ConfigStore.call(prefs(host), method, extras, true);
            if (configuration(result)) {
                accept(channel(result), generation, false);
                requestRefresh(host);
            } else {
                accept(channel(ConfigStore.call(prefs(host), "getConfig", null, true)), generation, false);
            }
            return result;
        }
    }
    static Bundle call(Context context, String method, int id) {
        Bundle extras = new Bundle(); extras.putInt("id", id); return mutate(context, method, extras);
    }
    static Bundle call(Context context, String method) { return mutate(context, method, null); }
    static Bundle saveSettings(Context context, boolean master, boolean random, boolean locked, int id, int[] pool) {
        return writeSettings(context, master, random, locked, null, id, pool);
    }
    static Bundle importSettings(Context context, boolean master, boolean random, boolean locked, Boolean anti, int id, int[] pool) {
        return writeSettings(context, master, random, locked, anti, id, pool);
    }
    private static Bundle writeSettings(Context context, boolean master, boolean random, boolean locked, Boolean anti, int id, int[] pool) {
        Bundle b = new Bundle(); b.putBoolean("masterEnabled", master); b.putBoolean("randomEnabled", random);
        b.putBoolean("lockedEnabled", locked); b.putInt("lockedId", id); b.putIntArray("pool", pool == null ? new int[0] : pool.clone());
        if (anti != null) b.putBoolean("antiRevokeEnabled", anti);
        return mutate(context, "saveSettings", b);
    }
    static Bundle saveAntiRevoke(Context context, boolean enabled) {
        Bundle extras = new Bundle(); extras.putBoolean("enabled", enabled); return mutate(context, "setAntiRevoke", extras);
    }
    static void stageSettings(boolean master, boolean random, boolean locked, int id, int[] pool) {
        synchronized (STATE) {
        GENERATION.incrementAndGet();
        Bundle staged = new Bundle(cache);
        staged.putBoolean("masterEnabled", master); staged.putBoolean("randomEnabled", random);
        staged.putBoolean("lockedEnabled", locked); staged.putInt("lockedId", id);
        staged.putIntArray("pool", pool == null ? new int[0] : pool.clone()); staged.putBoolean("_ok", true);
        cache = AppConfig.normalize(staged);
        }
    }
    static void stageAntiRevoke(boolean enabled) {
        synchronized (STATE) {
        GENERATION.incrementAndGet();
        Bundle staged = new Bundle(cache); staged.putBoolean("antiRevokeEnabled", enabled); cache = staged;
        }
    }
    static void runAsync(Runnable task) {
        long generation = GENERATION.get();
        WRITES.execute(() -> {
            WRITE_GENERATION.set(generation);
            try { task.run(); }
            catch (Throwable error) { HookLog.info("后台操作失败：" + error.getClass().getSimpleName()); }
            finally { WRITE_GENERATION.remove(); }
        });
    }
    static void noteApplied(Context context, int id) {
        if (!AppConfig.validId(id)) return;
        Context host = app(context); if (host == null) return;
        synchronized (STATE) {
            if (!cache.getBoolean("masterEnabled", false)) return;
            Bundle staged = new Bundle(cache); staged.putInt("lastAppliedId", id); cache = staged;
        }
        // Runtime statistics do not extend the lifetime of the source configuration.
        ScheduledFuture<?> next = WRITES.schedule(() -> {
            Bundle extra = new Bundle(); extra.putInt("id", id);
            ConfigStore.call(prefs(host), "recordApplied", extra, true);
        }, 900L, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> old = APPLIED_WRITE.getAndSet(next); if (old != null) old.cancel(false);
    }
    static void reportHook(Context context, String version) {
        long now = SystemClock.elapsedRealtime();
        if (lastHookReportTime > 0L && now - lastHookReportTime < 60_000L) return;
        lastHookReportTime = now;
        Context host = app(context); if (host == null) return;
        runAsync(() -> {
            Bundle extra = new Bundle(); extra.putString("version", version == null ? "" : version);
            synchronized (IO) {
                Bundle local = ConfigStore.call(prefs(host), "reportHook", extra, true);
                accept(synchronize(host, local, false), operationGeneration(), true);
                if (providerAvailable) {
                    try { host.getContentResolver().call(URI, "reportHook", null, extra); }
                    catch (Throwable ignored) {}
                }
            }
        });
    }
    static void appendLog(Context context, String message) {
        Context host = app(context); if (host == null) return;
        runAsync(() -> {
            Bundle extra = new Bundle(); extra.putString("message", message);
            ConfigStore.call(prefs(host), "appendLog", extra, true);
        });
    }
    static boolean isNoticeAcceptedV135(Context context) {
        if (get(context).getBoolean(AppConfig.QQ_NOTICE_V135, false)) return true;
        try { return context.getSharedPreferences(HOST_NOTICE_PREFS, Context.MODE_PRIVATE).getBoolean(AppConfig.QQ_NOTICE_V135, false); }
        catch (Throwable ignored) { return false; }
    }
    static boolean acceptNoticeV135(Context context) { return success(mutate(context, "acceptNoticeV36Final1", null)); }
    static Bundle healthCheck(Context context) {
        Context host = app(context); if (host == null) return Bundle.EMPTY;
        synchronized (IO) {
            Bundle result = ConfigStore.call(prefs(host), "healthCheck", null, true);
            if (!success(result)) return result;
            Bundle synced = synchronize(host, result, true);
            synced.putBoolean("_healthOk", result.getBoolean("_healthOk", false));
            synced.putBoolean("_callerAuthorized", true);
            accept(synced, operationGeneration(), true);
            return synced;
        }
    }
}
