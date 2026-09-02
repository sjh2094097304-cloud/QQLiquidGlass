package io.github.liuran001.mmliquidglass;

public final class DockOptionsTest {
    private static int checks;
    static void check(boolean ok,String detail) { checks++;if(!ok)throw new AssertionError(detail); }
    public static void main(String[] args) {
        DockOptions o=new DockOptions();
        for(DockOptions.Key key:DockOptions.Key.values()) {
            check(o.get(key)==key.initial,"default "+key);
            o.set(key,Integer.MIN_VALUE);check(o.get(key)==key.min,"lower bound "+key);
            o.set(key,Integer.MAX_VALUE);check(o.get(key)==key.max,"upper bound "+key);
            o.set(key,key.initial);
        }
        DockOptions draft=new DockOptions(o);draft.set(DockOptions.Key.TEXT,24);
        check(o.get(DockOptions.Key.TEXT)==17,"draft must not mutate live options");
        for(float density:new float[]{1,1.5f,2.625f,3,4})
            for(int width:new int[]{1,80,200,280,320,360,411,600,1200})
                for(int count:new int[]{3,4,5})
                    for(int scale:new int[]{75,100,125})
                        for(int percent:new int[]{60,90,100})
                            for(int showAvatar:new int[]{0,1})
                                for(int shift:new int[]{-40,0,40}) {
                                    o.set(DockOptions.Key.SCALE,scale);o.set(DockOptions.Key.WIDTH,percent);
                                    o.set(DockOptions.Key.AVATAR,showAvatar);o.set(DockOptions.Key.SHIFT,shift);
                                    int pixels=Math.round(width*density);
                                    DockGeometry g=new DockGeometry(pixels,density,count,o);
                                    check(g.left>=0,"left bounded");
                                    check(g.barWidth>0,"positive capsule");
                                    check(g.avatarLeft==g.left+g.barWidth+g.gap,"no overlap");
                                    check(g.avatarLeft+g.avatarSize<=pixels,"right bounded");
                                    if(showAvatar==0)check(g.avatarSize==0 && g.gap==0,"no ghost avatar gap");
                                }
        o.preset(1);
        check(o.get(DockOptions.Key.BLUR)==4 && o.get(DockOptions.Key.REFRACTION)==24
                && o.get(DockOptions.Key.SATURATION)==150 && o.get(DockOptions.Key.OPACITY)==40,"upstream optical preset");
        o.preset(0);check(o.get(DockOptions.Key.OPACITY)<40,"clear preset");
        o.preset(2);check(o.get(DockOptions.Key.OPACITY)==100 && o.get(DockOptions.Key.BLUR)==0
                && o.get(DockOptions.Key.REFRACTION)==0,"solid preset disables optics");
        System.out.println("PASS: "+checks+" settings bounds/draft/geometry assertions");
    }
}
