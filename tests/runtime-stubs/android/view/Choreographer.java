package android.view;
public final class Choreographer {
    public interface FrameCallback {void doFrame(long ns);}
    private static final Choreographer INSTANCE=new Choreographer();
    private final java.util.Set<FrameCallback> queue=new java.util.LinkedHashSet<>();
    private long now;
    public static Choreographer getInstance(){return INSTANCE;}
    public void postFrameCallback(FrameCallback c){queue.add(c);}
    public void removeFrameCallback(FrameCallback c){queue.remove(c);}
    public int pending(){return queue.size();}
    public void frames(int count){for(int i=0;i<count;i++){now+=16666667;android.os.SystemClock.now=now/1000000;
        java.util.List<FrameCallback> callbacks=new java.util.ArrayList<>(queue);queue.clear();
        for(FrameCallback c:callbacks)c.doFrame(now);}}
}
