package io.github.liuran001.mmliquidglass;

public final class UiWorkGateTest {
    public static void main(String[] args) {
        UiWorkGate gate = new UiWorkGate();
        int posts = 0;
        for (int i = 0; i < 100000; i++) if (gate.request()) posts++;
        require(posts == 1, "layout flood must queue just one task");
        gate.complete();
        require(gate.request(), "can enqueue after completion");
        gate.cancel();
        require(gate.request(), "can enqueue after cancelled pause");
        long first = gate.beginAction(0);
        require(first >= 0, "first click accepted");
        for (int i = 0; i < 1000; i++) require(gate.beginAction(i) == -1, "rapid click/reentry rejected");
        require(gate.claimAction(first), "one native header click");
        for (int i = 0; i < 10000; i++) require(!gate.claimAction(first), "no retries after native click");
        long second = gate.beginAction(1000);
        require(second > first, "next physical click gets a new token");
        require(!gate.claimAction(first), "old callbacks cannot click");
        gate.cancel();
        require(!gate.claimAction(second), "pause invalidates pending click");
        require(!gate.isCurrent(second), "pause invalidates stale work");
        System.out.println("PASS: work coalescing (100000 events), click reentry/cooldown, single native call, pause cancellation");
    }
    private static void require(boolean ok, String reason) {
        if (!ok) throw new AssertionError(reason);
    }
}
