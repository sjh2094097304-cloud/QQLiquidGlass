package com.qiutian.bianpaobubble.hook;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Shuffle-bag selector: every valid bubble ID is used exactly once per round.
 * The next round is shuffled again and its first ID never equals the previous result.
 */
final class BubbleRandomBag {
    private int[] bag = new int[0];
    private int index;
    private int[] source = new int[0];
    private int lastId;

    synchronized int next(int[] pool, int persistedLastId) {
        if (pool == null || pool.length == 0) { source = new int[0]; bag = new int[0]; index = 0; return 0; }
        boolean changed = !Arrays.equals(source, pool);
        if (changed) {
            source = pool.clone();
            bag = uniquePositive(pool);
            index = bag.length;
        }
        if (bag.length == 0) return 0;
        if (lastId == 0 && persistedLastId >= 1000) lastId = persistedLastId;
        if (index >= bag.length) {
            index = 0;
            shuffle(bag);
            avoidBoundaryRepeat(bag, lastId);
        }

        int selected = bag[index++];
        lastId = selected;
        return selected;
    }

    synchronized void noteApplied(int id) { if (id >= 1000) lastId = id; }

    synchronized void resetForTest() {
        bag = new int[0];
        index = 0;
        source = new int[0];
        lastId = 0;
    }

    private static int[] uniquePositive(int[] pool) {
        if (pool == null || pool.length == 0) return new int[0];
        Set<Integer> values = new LinkedHashSet<>();
        for (int id : pool) if (id >= 1000) values.add(id);
        int[] result = new int[values.size()];
        int offset = 0;
        for (Integer value : values) result[offset++] = value;
        return result;
    }

    private static void shuffle(int[] values) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = values.length - 1; i > 0; i--) {
            int other = random.nextInt(i + 1);
            int value = values[i];
            values[i] = values[other];
            values[other] = value;
        }
    }

    private static void avoidBoundaryRepeat(int[] values, int previous) {
        if (values.length < 2 || previous <= 0 || values[0] != previous) return;
        int swapIndex = 1 + ThreadLocalRandom.current().nextInt(values.length - 1);
        int value = values[0];
        values[0] = values[swapIndex];
        values[swapIndex] = value;
    }
}
