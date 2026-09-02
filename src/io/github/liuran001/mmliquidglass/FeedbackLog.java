package io.github.liuran001.mmliquidglass;

import java.util.ArrayDeque;
import java.util.Locale;

/** Bounded in-memory event log. Never accepts chat text or account identifiers. */
final class FeedbackLog {
    static final String VERSION="0.4.2-qqdock.6";
    static volatile boolean enabled=true;
    private static final ArrayDeque<String> lines=new ArrayDeque<>();
    private static String last="";
    static String sanitize(String value) {
        if(value==null) return "";
        return value.replaceAll("https?://\\S+","[URL]")
                .replaceAll("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}","[EMAIL]")
                .replaceAll("(?<![0-9])[0-9]{6,}(?![0-9])","[ID]")
                .replace('\n',' ').replace('\r',' ');
    }
    static synchronized void event(String code,String detail) {
        if(!enabled) return;
        String text=code+" "+sanitize(detail);
        if(text.equals(last)) return;
        last=text;
        if(text.length()>240) text=text.substring(0,240);
        long seconds=System.currentTimeMillis()/1000;
        lines.addLast(String.format(Locale.ROOT,"%02d:%02d:%02d %s",seconds/3600%24,seconds/60%60,seconds%60,text));
        while(lines.size()>160) lines.removeFirst();
    }
    static void error(String code,Throwable t) {
        event(code,t==null?"unknown":t.getClass().getName()); // no Throwable.getMessage()
    }
    static synchronized String text() {
        StringBuilder s=new StringBuilder();
        for(String line:lines) s.append(line).append('\n');
        return s.length()==0?"暂无事件。日志仅保存在当前 QQ 进程内。\n":s.toString();
    }
    static synchronized void clear() { lines.clear(); last=""; }
}
