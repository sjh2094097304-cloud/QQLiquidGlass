package io.github.liuran001.mmliquidglass;

public final class FeedbackLogTest {
    private static void check(boolean ok,String detail){if(!ok)throw new AssertionError(detail);}
    public static void main(String[] args){
        String cleaned=FeedbackLog.sanitize("account=123456789 email=a@example.com https://example.com/p/123456789\nhello");
        check(!cleaned.contains("123456789")&&!cleaned.contains("a@example.com")&&!cleaned.contains("https://"),"redaction");
        check(!cleaned.contains("\n"),"no injected log line");
        FeedbackLog.clear();FeedbackLog.enabled=true;
        for(int i=0;i<1000;i++)FeedbackLog.event("EVENT","item "+i);
        check(FeedbackLog.text().split("\n").length==160,"bounded log");
        String before=FeedbackLog.text();FeedbackLog.event("EVENT","item 999");check(before.equals(FeedbackLog.text()),"deduplication");
        FeedbackLog.enabled=false;FeedbackLog.event("NO","must not be logged");check(before.equals(FeedbackLog.text()),"logging disabled");
        FeedbackLog.enabled=true;FeedbackLog.error("ERR",new RuntimeException("secret-account-123456789"));
        check(!FeedbackLog.text().contains("secret-account"),"no exception message leakage");
        FeedbackLog.clear();check(FeedbackLog.text().contains("暂无事件"),"clear");
        System.out.println("PASS: bounded logs, redaction, deduplication, disable/clear and exception privacy");
    }
}
