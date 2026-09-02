package io.github.liuran001.mmliquidglass;

/** UI-thread gate: coalesce work and permit only one native action per gesture. */
final class UiWorkGate {
    private boolean pending;
    private long generation;
    private long nextActionAt;
    private boolean actionIssued;

    boolean request() {
        if (pending) return false;
        pending = true;
        return true;
    }
    void complete() { pending = false; }
    long cancel() { pending = false; actionIssued = true; return ++generation; }
    long beginAction(long now) {
        if (now < nextActionAt) return -1;
        nextActionAt = now + 1000;
        actionIssued = false;
        return ++generation;
    }
    boolean isCurrent(long token) { return token == generation; }
    boolean claimAction(long token) {
        if (token != generation || actionIssued) return false;
        actionIssued = true;
        return true;
    }
}
