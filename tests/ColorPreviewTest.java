package io.github.liuran001.mmliquidglass;
public final class ColorPreviewTest {
    private static int checks;
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    public static void main(String[] args) {
        check(RgbColor.parse("#000000")==0,"black is a valid color, not missing");
        check(RgbColor.parse(" #aBcDeF ")==0xabcdef,"case and whitespace");
        check(RgbColor.parse("#6cf")==0x66ccff,"shorthand");
        for(String invalid:new String[]{"","#","66","#66cc","#GGDDFF","#AA66CCFF","red","１２３４５６"})check(RgbColor.parse(invalid)==null,"incomplete or invalid input stays uncommitted");
        DockOptions saved=new DockOptions();
        int[] palette={0xff4b93ff,0xff8d79ed,0xff19ae91,0xffea9b42};
        for(int i=0;i<4;i++){saved.set(DockOptions.Key.ACCENT,i);check(saved.accent()==palette[i],"existing palette remains unchanged");}
        DockOptions draft=new DockOptions(saved);draft.set(DockOptions.Key.ACCENT,4);
        for(int rgb:new int[]{0,1,0xffffff,0x66ccff,0xabcdef,0xff0000}) {
            draft.set(DockOptions.Key.CUSTOM_ACCENT,rgb);check(draft.accent()==(0xff000000|rgb),"opaque custom RGB");
            check(RgbColor.parse(RgbColor.format(rgb))==rgb,"hex round trip");
            DockOptions restored=new DockOptions(draft);check(restored.accent()==draft.accent(),"draft copy retains custom color");
        }
        check(saved.accent()==palette[3],"preview changes do not alter saved color");
        draft.preset(1);check(draft.get(DockOptions.Key.ACCENT)==4 && draft.get(DockOptions.Key.CUSTOM_ACCENT)==0xff0000,"optical preset retains custom accent");
        for(float d:new float[]{1,1.5f,2.625f,3,4})for(int h:new int[]{120,168,256})for(int common:new int[]{40,56,88,110}) {
            int previous=-1;
            for(int offset=0;offset<=100;offset++) {
                int actual=PreviewGeometry.offset(Math.round(h*d),Math.round(common*d),d,offset);
                check(actual>=previous,"lift preview is monotonic");previous=actual;
                check(actual>=0 && actual+Math.round(common*d)<=Math.round(h*d),"full-size bar stays in preview");
                if(h==256)check(Math.abs(actual-offset*d)<=.51f,"expanded viewport preserves full lift range");
            }
        }
        System.out.println("PASS: "+checks+" custom color/draft/preview geometry assertions");
    }
}
