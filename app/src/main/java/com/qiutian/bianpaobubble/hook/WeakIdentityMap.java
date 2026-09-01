package com.qiutian.bianpaobubble.hook;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

/** Weak keys compared by identity, even when QQ menu items override equals/hashCode. */
final class WeakIdentityMap<V> {
    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();
    private final Map<Key, V> entries = new LinkedHashMap<>();
    private final int limit;
    WeakIdentityMap(int limit) { this.limit = limit; }
    synchronized void put(Object key, V value) {
        clean();
        entries.put(new Key(key, queue), value);
        while (entries.size() > limit) entries.remove(entries.keySet().iterator().next());
    }
    synchronized V get(Object key) { clean(); return key == null ? null : entries.get(new Key(key, null)); }
    synchronized boolean containsKey(Object key) { return get(key) != null; }
    private void clean() {
        Key dead;
        while ((dead = (Key) queue.poll()) != null) entries.remove(dead);
    }
    private static final class Key extends WeakReference<Object> {
        private final int hash;
        Key(Object value, ReferenceQueue<Object> queue) { super(value, queue); hash = System.identityHashCode(value); }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object other) {
            if (this == other) return true;
            Object value = get();
            return value != null && other instanceof Key && value == ((Key) other).get();
        }
    }
}
