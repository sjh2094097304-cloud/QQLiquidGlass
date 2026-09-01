package com.qiutian.bianpaobubble.hook;

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import com.qiutian.bianpaobubble.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Regressions exercise real production logic with simulated Android storage and IPC. */
public final class RegressionChecks {
    private static final String LOCAL = "bianbian_bubble_host_config_v36";
    private static int checks;
    public static void main(String[] args) throws Exception {
        protoPackets();
        imports();
        randomBags();
        messageFilter();
        mallFilter();
        ownershipAndLogBounds();
        storageTransactions();
        providerRecovery();
        legacyMigration();
        cacheFreshnessAndCopies();
        cacheErrorRecovery();
        generationAndNonblockingRead();
        hookDiagnostics();
        reset();
        System.out.println("PASS: " + checks + " checks; production Java logic executed on JDK 17.");
        System.out.println("LIMIT: Android/Binder/QQ devices and APK packaging are not exercised by this suite.");
    }
    private static void check(boolean value, String name) {
        if (!value) throw new AssertionError(name);
        checks++;
    }
    private static void rejects(Runnable task, String name) {
        try { task.run(); } catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("accepted invalid input: " + name);
    }
    private static byte[] sync(byte[] body) { return ProtoLite.field(8, ProtoLite.field(4, body)); }
    private static byte[] message(int type, int sub) {
        return ProtoLite.field(2, ProtoLite.concat(ProtoLite.field(1, type), ProtoLite.field(2, sub)));
    }
    private static void protoPackets() {
        byte[] normal = message(82, 0), group = message(732, 17), c2c = message(528, 138);
        byte[] unknown = ProtoLite.concat(ProtoLite.field(77, 12L), ProtoLite.field(79, new byte[]{1,2,3}));
        byte[] normalField = ProtoLite.field(8, normal);
        byte[] before = sync(ProtoLite.concat(unknown, normalField, ProtoLite.field(8, group), normalField, ProtoLite.field(8, c2c)));
        ProtoLite.RewriteResult result = ProtoLite.stripSyncRecall(before);
        check(result.changed && Arrays.equals(result.bytes, sync(ProtoLite.concat(unknown, normalField, normalField))),
                "mixed sync preserves ordinary messages and unknown fields");
        byte[] ordinary = sync(ProtoLite.concat(unknown, normalField));
        result = ProtoLite.stripSyncRecall(ordinary);
        check(!result.changed && result.bytes == ordinary, "unchanged packets reuse original bytes");
        check(ProtoLite.isRecallMsgPush(ProtoLite.field(1, group)), "group recall recognized");
        check(ProtoLite.isRecallMsgPush(ProtoLite.field(1, c2c)), "C2C recall recognized");
        check(!ProtoLite.isRecallMsgPush(ProtoLite.field(1, message(732, 18))), "unrecognized subtype preserved");
        byte[] uid = "u_self".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] self = ProtoLite.concat(ProtoLite.field(1, ProtoLite.concat(ProtoLite.field(2, uid), ProtoLite.field(6, uid))), c2c);
        check(ProtoLite.isSelfMsgPush(ProtoLite.field(1, self)), "self routing recognized");
        result = ProtoLite.stripSyncRecall(sync(ProtoLite.concat(ProtoLite.field(8, self), ProtoLite.field(8, c2c))));
        check(Arrays.equals(result.bytes, sync(ProtoLite.field(8, self))), "self C2C recall preserved in a mixed sync batch");
        rejects(() -> ProtoLite.stripSyncRecall(new byte[]{66,100,0}), "truncated wire payload");
        rejects(() -> ProtoLite.stripSyncRecall(new byte[]{0}), "zero protobuf tag");
        rejects(() -> ProtoLite.stripSyncRecall(new byte[]{(byte)128,(byte)128,(byte)128,(byte)128,(byte)128,(byte)128,(byte)128,(byte)128,(byte)128,2}), "varint overflow");
        rejects(() -> ProtoLite.stripSyncRecall(new byte[4 * 1024 * 1024 + 1]), "packet size budget");
        System.out.println("OK protobuf: mixed batches, ordinary fields, self routing, malformed packet bounds");
    }
    private static void imports() {
        String encoded = ConfigCodec.encode("名字\"\\\n测试",true,false,true,0,Arrays.asList(1000,2000,1000));
        ConfigCodec.DecodedConfig value = ConfigCodec.decode(encoded);
        check(value.randomEnabled && value.antiRevokeEnabled && value.hasAntiRevokeSetting && value.ids.equals(Arrays.asList(1000,2000)), "JSON roundtrip and dedupe");
        check(value.name.equals("名字\"\\\n测试"), "escaped name roundtrip");
        for (String input : new String[]{"{this is not JSON}", "{}", "{\"mode\":\"random\"}",
                "{\"mode\":\"independent\",\"lockedId\":1234.56}", "{\"mode\":\"random\",\"pool\":[1000.5]}",
                "{\"mode\":\"random\",\"pool\":\"1000\"}", "{\"mode\":\"random\",\"pool\":[true]}",
                "{\"mode\":\"off\",\"pool\":[],\"antiRevokeEnabled\":\"false\"}",
                "{\"mode\":\"off\",\"pool\":[]} trailing", "[1000,]", "[1000.2]", "999", "0001", "2147483648", "1000abc", "1000/2000"}) {
            rejects(() -> ConfigCodec.decode(input), input);
        }
        check(!ConfigCodec.decode("1000，2000;1000\n3000").hasAntiRevokeSetting, "ID-only import leaves unrelated protection unspecified");
        check(ConfigCodec.decode("[1000,\"2000\"]").ids.size() == 2, "legacy JSON list accepts numeric strings");
        check(!ConfigCodec.decode("{\"mode\":\"off\",\"pool\":[]}").randomEnabled, "explicit empty off configuration accepted");
        check(ConfigCodec.decode("{\"mode\":\"independent\",\"lockedId\":1000}").ids.equals(Collections.singletonList(1000)), "fixed ID is included in pool");
        StringBuilder ids = new StringBuilder();
        for(int i=0;i<300;i++) ids.append(i+1000).append(' ');
        check(ConfigCodec.decode(ids.toString()).ids.size()==300, "300-ID limit inclusive");
        String tooMany = ids + "2000";
        rejects(() -> ConfigCodec.decode(tooMany), "301 IDs");
        String nested = "{\"mode\":\"off\",\"pool\":[],\"unknown\":" + "[".repeat(20) + "0" + "]".repeat(20) + "}";
        rejects(() -> ConfigCodec.decode(nested), "deep nesting cannot overflow parser stack");
        System.out.println("OK import: real Android JSON implementation, malformed data, limits, escapes, omitted fields");
    }
    private static void randomBags() {
        BubbleRandomBag bag = new BubbleRandomBag();
        int[] first = {1000,2000}, collision = {1001,1969};
        check(Arrays.hashCode(first)==Arrays.hashCode(collision), "known array hash collision");
        bag.next(first,0);
        int id=bag.next(collision,0);
        check(id==1001||id==1969,"pool replacement cannot reuse old hash-colliding IDs");
        bag = new BubbleRandomBag();
        int[] pool={1000,2000,3000,4000,1000,0,115};
        int last=0;
        for(int round=0;round<120;round++) {
            Set<Integer> seen=new HashSet<>();
            for(int n=0;n<4;n++) {
                int selected=bag.next(pool,1000); // Deliberately stale persisted statistics.
                check(selected!=last,"consecutive random picks differ");
                check(seen.add(selected),"one occurrence per unique-ID round");
                last=selected;
            }
            check(seen.equals(new HashSet<>(Arrays.asList(1000,2000,3000,4000))),"all valid IDs used each round");
        }
        check(bag.next(new int[]{0,115},0)==0,"invalid pool yields no ID");
        check(bag.next(new int[]{1000},0)==1000 && bag.next(new int[]{1000},1000)==1000,"single-ID pool remains valid");
        check(bag.next(null,0)==0,"null pool safely resets");
        bag=new BubbleRandomBag(); bag.noteApplied(1000);
        check(bag.next(new int[]{1000,2000},0)==2000,"fixed-to-random transition avoids last applied bubble");
        System.out.println("OK random: hash-collision regression and 120 shuffled rounds");
    }
    public static class Bubble { public Integer bubbleId,subBubbleId; Bubble(int main,int sub){bubbleId=main;subBubbleId=sub;} }
    public static class Vas { public Bubble bubbleInfo; Vas(Bubble b){bubbleInfo=b;} }
    public static class Attr { public Vas vasMsgInfo; Attr(Bubble b){vasMsgInfo=new Vas(b);} }
    public static class Text { public String content; Text(String s){content=s;} }
    public static class Element { public Text textElement; public Object faceElement,marketFaceElement,faceBubbleElement,giphyElement; }
    public static class Record { public List<Element> elements=new ArrayList<>(); public Map<Integer,Attr> msgAttrs=new HashMap<>(); public List<Record> records; public int bubbleId=999999; }
    public static class Wrapper { public Record data; Wrapper(Record r){data=r;} }
    private static Record record(int main,int sub,String text) {
        Record r=new Record();r.msgAttrs.put(0,new Attr(new Bubble(main,sub)));
        Element e=new Element();if(text!=null)e.textElement=new Text(text);r.elements.add(e);return r;
    }
    private static void messageFilter() {
        check(Reflector.bubbleId(new Wrapper(record(1000,2000,"文字")))==2000,"current text bubble from VAS metadata");
        check(Reflector.bubbleId(record(1000,0,"文字"))==1000,"main bubble fallback");
        check(Reflector.bubbleId(record(0,2000,"文字"))==0,"explicit default main ID rejects stale sub-ID");
        check(Reflector.bubbleId(record(115,2000,"bubbleId=3333333"))==0,"default 115 and message text cannot create IDs");
        check(Reflector.bubbleId("{\"bubbleId\":1234567}")==0,"message strings are never scanned");
        check(Reflector.bubbleId(Collections.singletonMap("bubble_id",1234567))==0,"untyped maps are never scanned");
        Record face=record(1000,2000,null);face.elements.get(0).faceElement=new Object();
        check(Reflector.bubbleId(face)==0,"standalone face filtered despite VAS metadata");
        Record sticker=record(1000,2000,"[表情]");sticker.elements.get(0).marketFaceElement=new Object();
        check(Reflector.bubbleId(sticker)==0,"sticker placeholder text filtered");
        Record forwarded=record(0,0,"转发");forwarded.records=Collections.singletonList(record(1000,2000,"源消息"));
        check(Reflector.bubbleId(forwarded)==0,"forwarded child bubble is not the current bubble");
        Record inline=record(1000,2000,"文字 + 表情");inline.elements.get(0).faceElement=new Object();
        check(Reflector.bubbleId(inline)==2000,"real text with inline face retains its bubble");
        check(Reflector.bubbleInfoFromArgs(new Object[]{"bubbleId=12345",inline.msgAttrs})!=null,"send path only follows attribute maps");
        check(Reflector.positiveInt(1234.5)==0 && Reflector.positiveInt("2147483648")==0,"numeric parsing never truncates");
        System.out.println("OK message filter: default, emoji, stickers, quoted and forwarded IDs");
    }
    private static void mallFilter() {
        check(MallIdParser.parse("https://qq.example/?itemid=2_12345&foo=1")==12345,"typed query accepted");
        check(MallIdParser.parse("{\"item_id\":\"2_12345\"}")==12345,"typed JSON marker accepted");
        check(MallIdParser.parse("itemid%253D2_12345")==12345,"double encoded marker accepted");
        for(String input:new String[]{"itemid=12345","itemid=1_12345","notitemid=2_12345","itemid=2_12345x",
                "itemid=2_1234567890123","itemid=2_115","itemid=2_2147483648",
                "itemid=2_12345&item_id=2_23456","itemid=1_55555&kr_turbo_display=2_12345","just bubble id 12345"}) {
            check(MallIdParser.parse(input)==0,"mall rejects "+input);
        }
        check(MallIdParser.parse("itemid=99999&kr_turbo_display=2_12345")==12345,"bare unrelated ID cannot override explicit bubble ID");
        check(MallIdParser.parse("itemid=2_12345x&kr_turbo_display=2_23456")==0,"malformed prefixed marker cannot hide a conflict");
        MallIdParser.Selection selected=new MallIdParser.Selection();
        MallIdParser.collectValue("itemid","99999",selected);
        MallIdParser.collectValue("kr_turbo_display","2_12345",selected);
        check(selected.value()==12345,"bundle and URL share the same evidence rules");
        check(MallIdParser.direct("2_12345")==12345 && MallIdParser.direct("x2_12345")==0 && MallIdParser.direct("12345")==0,"direct IDs need exact type prefix");
        System.out.println("OK mall filter: prefixes, boundaries, URL encoding, conflicting products");
    }
    public static class EqualItem { @Override public int hashCode(){return 1;} @Override public boolean equals(Object other){return other instanceof EqualItem;} }
    private static void ownershipAndLogBounds() throws Exception {
        WeakIdentityMap<String> map=new WeakIdentityMap<>(4);
        EqualItem first=new EqualItem(),second=new EqualItem();map.put(first,"first");
        check(!map.containsKey(second),"QQ value-equal native menu item is not owned");
        map.put(second,"second");
        check("first".equals(map.get(first)) && "second".equals(map.get(second)),"menu actions remain separate");
        de.robv.android.xposed.XposedBridge.logs.clear();
        Field recent=HookLog.class.getDeclaredField("RECENT");recent.setAccessible(true);((Map<?,?>)recent.get(null)).clear();
        SystemClock.now=100_000;
        HookLog.error(null,"same",new IllegalStateException("a"));
        // Keep the same stack trace to exercise the actual duplicate key.
        Throwable error=new IllegalStateException("duplicate");
        HookLog.error(null,"repeat",error);int logs=de.robv.android.xposed.XposedBridge.logs.size();
        SystemClock.now+=30_000;HookLog.error(null,"repeat",error);
        SystemClock.now+=30_001;HookLog.error(null,"repeat",error);
        check(de.robv.android.xposed.XposedBridge.logs.size()==logs+1,"ignored duplicates do not extend suppression forever");
        for(int n=0;n<250;n++)HookLog.error(null,"entry-"+n,new IllegalStateException("x"));
        check(((Map<?,?>)recent.get(null)).size()<=128,"duplicate log memory is bounded");
        System.out.println("OK menu identity and bounded duplicate logging");
    }
    private static Bundle settings(int id,int... pool) {
        Bundle value=new Bundle();value.putBoolean("masterEnabled",id!=0);value.putBoolean("randomEnabled",false);
        value.putBoolean("lockedEnabled",id!=0);value.putInt("lockedId",id);value.putIntArray("pool",pool);return value;
    }
    private static Bundle operation(String key,int id) {Bundle b=new Bundle();b.putInt(key,id);return b;}
    private static void storageTransactions() throws Exception {
        reset();Environment e=new Environment();
        Bundle result=e.provider.call("saveSettings",null,settings(1000,1000,2000));
        check(HostConfig.success(result),"provider settings stored");
        result=e.provider.call("removeBubble",null,operation("id",1000));
        check(!result.getBoolean("lockedEnabled",true)&&result.getInt("lockedId",-1)==0&&Arrays.equals(result.getIntArray("pool"),new int[]{2000}),"removing active fixed ID disables it in provider");
        MemoryPrefs prefs=e.module.prefs(AppConfig.PREFS);
        prefs.edit().putString("pool","[2000,3000]").putBoolean("masterEnabled",false)
                .putBoolean("randomEnabled",true).putInt("lockedId",2000).commit();
        result=e.provider.call("removeBubble",null,operation("id",2000));
        check(!result.getBoolean("masterEnabled",true),"deleting an ID cannot resurrect a disabled legacy master switch");
        e.provider.call("saveSettings",null,settings(1000,1000));prefs.failCommit=true;
        result=e.provider.call("saveSettings",null,settings(2000,2000));
        check(!HostConfig.success(result)&&result.containsKey("_error"),"failed commit returns explicit failure");
        check(prefs.getInt("lockedId",0)==1000,"failed commit rolls back in-memory values");
        prefs.restart();check(prefs.getInt("lockedId",0)==1000,"failed commit retains durable values");
        result=e.provider.call("healthCheck",null,null);
        check(!result.getBoolean("_healthOk",true),"provider health probe detects failed disk writes");
        e.offline=true;MemoryPrefs local=e.host.prefs(LOCAL);local.failCommit=true;
        result=HostConfig.healthCheck(e.host);
        check(!result.getBoolean("_healthOk",true)&&local.commits>0,"fallback health performs real failed write probe");
        local.failCommit=false;result=HostConfig.healthCheck(e.host);
        check(result.getBoolean("_healthOk",false)&&!local.memory.containsKey("_healthProbe"),"healthy probe cleans up its key");
        android.os.Binder.callingUid=2000;android.content.pm.PackageManager.packages=new String[]{"unknown.app"};
        check(e.provider.call("getConfig",null,null).getBoolean("_authDenied",false),"unknown UID rejected by provider");
        android.content.pm.PackageManager.packages=new String[]{"com.tencent.mobileqq"};
        check(HostConfig.success(e.provider.call("getConfig",null,null)),"QQ UID allowed");
        android.os.Binder.callingUid=1000;
        System.out.println("OK storage: delete-fixed, commit rollback, actual health probe, caller authorization");
    }
    private static void providerRecovery() throws Exception {
        reset();Environment e=new Environment();e.offline=true;
        Bundle saved=HostConfig.saveSettings(e.host,true,false,true,2000,new int[]{2000,3000});drain();
        check(HostConfig.success(saved)&&e.host.prefs(LOCAL).getBoolean("_pendingSync",false),"offline changes durably queued");
        e.provider.call("saveSettings",null,settings(1000,1000));
        e.offline=false;SystemClock.now+=310_000;HostConfig.refresh(e.host);drain();
        check(HostConfig.get(e.host).getInt("lockedId",0)==2000,"recovery does not restore old remote fixed ID");
        check(e.module.prefs(AppConfig.PREFS).getInt("lockedId",0)==2000&&!e.host.prefs(LOCAL).getBoolean("_pendingSync",true),"local replay acknowledged after provider commit");
        HostConfig.stageSettings(false,false,false,0,new int[]{3000});HostConfig.call(e.host,"removeBubble",2000);drain();
        check(!HostConfig.get(e.host).getBoolean("lockedEnabled",true)&&HostConfig.get(e.host).getInt("lockedId",-1)==0,"host and provider share delete-fixed rules");
        HostConfig.saveAntiRevoke(e.host,true);drain();
        HostConfig.importSettings(e.host,true,true,false,null,0,new int[]{3000});drain();
        check(HostConfig.get(e.host).getBoolean("antiRevokeEnabled",false),"ID-only import preserves anti-recall setting");
        e.offline=true;HostConfig.saveSettings(e.host,true,false,true,4000,new int[]{4000});drain();
        e.host.prefs(LOCAL).restart();cacheField("cache").set(null,Bundle.EMPTY);
        check(HostConfig.get(e.host).getInt("lockedId",0)==4000,"local offline changes survive restart");drain();
        e.offline=false;e.module.prefs(AppConfig.PREFS).failCommit=true;SystemClock.now+=310_000;HostConfig.refresh(e.host);
        check(e.host.prefs(LOCAL).getBoolean("_pendingSync",false)&&HostConfig.get(e.host).getInt("lockedId",0)==4000,"failed remote replay retains pending local settings");
        System.out.println("OK provider recovery: durable local replay, retries, restart, partial imports");
    }
    private static void legacyMigration() throws Exception {
        reset();Environment e=new Environment();
        e.module.prefs(AppConfig.PREFS).edit().putString("pool","[1000]").putInt("lockedId",1000).putBoolean("lockedEnabled",true).commit();
        e.host.prefs(LOCAL).edit().putString("pool","[2000,3000]").putInt("lockedId",2000).putBoolean("lockedEnabled",true).commit();
        HostConfig.refresh(e.host);drain();
        check(HostConfig.get(e.host).getInt("lockedId",0)==2000&&e.module.prefs(AppConfig.PREFS).getInt("lockedId",0)==2000,"3.6 local copy without revision/dirty marker survives first 3.7 sync");
        int before=e.host.prefs(LOCAL).commits;
        HostConfig.refresh(e.host);drain();
        check(e.host.prefs(LOCAL).commits==before,"unchanged provider snapshot avoids redundant disk writes");
        Bundle snapshot=ConfigStore.call(e.host.prefs(LOCAL),"getConfig",null,true);
        ConfigStore.call(e.host.prefs(LOCAL),"saveSettings",settings(4000,4000),true);
        check(!ConfigStore.mirror(e.host.prefs(LOCAL),snapshot,true,snapshot.getLong("settingsRevision",0L))
                && e.host.prefs(LOCAL).getInt("lockedId",0)==4000,"stale mirror cannot overwrite a concurrent local writer");
        System.out.println("OK 3.6 migration and unchanged-snapshot write suppression");
    }
    private static void cacheFreshnessAndCopies() throws Exception {
        reset();Environment e=new Environment();HostConfig.saveSettings(e.host,true,false,true,1000,new int[]{1000,2000});drain();
        long readAt=cacheField("sourceReadAt").getLong(null);
        e.provider.call("saveSettings",null,settings(2000,2000));
        int calls=e.calls;
        SystemClock.now+=19_000;HostConfig.noteApplied(e.host,1000);cancelApplied();
        check(cacheField("sourceReadAt").getLong(null)==readAt,"send statistics do not renew source TTL");
        check(HostConfig.getForSend(e.host).getInt("lockedId",0)==1000&&e.calls==calls,"fresh cache does not perform IPC");
        SystemClock.now+=2_000;HostConfig.getForSend(e.host);drain();
        check(HostConfig.get(e.host).getInt("lockedId",0)==2000,"expired source snapshot refreshes even during sends");
        Bundle copy=HostConfig.get(e.host);copy.getIntArray("pool")[0]=99999;copy.putInt("lockedId",99999);
        check(HostConfig.get(e.host).getInt("lockedId",0)==2000&&HostConfig.get(e.host).getIntArray("pool")[0]==2000,"callers cannot modify cached snapshots");
        System.out.println("OK cache age and defensive snapshot copies");
    }
    private static void cacheErrorRecovery() throws Exception {
        reset();Environment e=new Environment();e.offline=true;HostConfig.saveSettings(e.host,true,false,true,1000,new int[]{1000});drain();
        e.host.prefs(LOCAL).memory.put("masterEnabled","wrong-type");
        HostConfig.refresh(e.host);
        check(HostConfig.getForSend(e.host).getInt("lockedId",0)==1000&&HostConfig.getForSend(e.host).getBoolean("masterEnabled",false),"error bundle cannot replace last good cache");
        e.host.prefs(LOCAL).memory.put("masterEnabled",true);
        e.host.prefs(LOCAL).failCommit=true;
        HostConfig.stageSettings(true,false,true,2000,new int[]{2000});
        Bundle failed=HostConfig.saveSettings(e.host,true,false,true,2000,new int[]{2000});
        check(!HostConfig.success(failed)&&HostConfig.get(e.host).getInt("lockedId",0)==1000,"failed staged write restores previous valid config");
        System.out.println("OK read failures and staged-write rollback");
    }
    private static void generationAndNonblockingRead() throws Exception {
        reset();Environment cold=new Environment();
        cold.entered=new CountDownLatch(1);cold.release=new CountDownLatch(1);
        HostConfig.reportHook(cold.host,"9.2.75");
        check(cold.entered.await(3,TimeUnit.SECONDS),"cold-start provider probe started");
        ExecutorService firstSend=Executors.newSingleThreadExecutor();
        try {
            check(firstSend.submit(() -> HostConfig.getForSend(cold.host)).get(1,TimeUnit.SECONDS).getIntArray("pool")!=null,
                    "first send initializes from local storage without waiting for startup IPC");
        } finally { cold.release.countDown();firstSend.shutdownNow();drain(); }
        reset();Environment e=new Environment();HostConfig.saveSettings(e.host,true,false,true,1000,new int[]{1000});drain();
        e.entered=new CountDownLatch(1);e.release=new CountDownLatch(1);
        HostConfig.runAsync(() -> HostConfig.refresh(e.host));
        check(e.entered.await(3,TimeUnit.SECONDS),"slow provider started");
        ExecutorService caller=Executors.newSingleThreadExecutor();
        try {
            Future<Bundle> read=caller.submit(() -> HostConfig.getForSend(e.host));
            check(read.get(1,TimeUnit.SECONDS).getInt("lockedId",0)==1000,"send reads do not wait for slow IPC");
            HostConfig.stageSettings(true,false,true,2000,new int[]{2000});
            e.release.countDown();drain();
            check(HostConfig.getForSend(e.host).getInt("lockedId",0)==2000,"older async read cannot overwrite later staged settings");
            e.entered=null;e.release=null;
            HostConfig.saveSettings(e.host,true,false,true,2000,new int[]{2000});drain();
            check(e.module.prefs(AppConfig.PREFS).getInt("lockedId",0)==2000,"latest edit eventually persisted and synchronized");
        } finally {e.release=null;caller.shutdownNow();}
        System.out.println("OK concurrency: slow IPC, nonblocking send reads, stale response rejection");
    }
    private static void hookDiagnostics() {
        check(!HookStatus.requiredReady(false),"uninitialized hooks cannot pass diagnostics");
        HookStatus.started();
        for(String name:new String[]{"发送气泡","长按菜单","商城识别"})HookStatus.installed(name);
        check(HookStatus.requiredReady(false)&&!HookStatus.requiredReady(true),"enabled anti-recall requires its own installed hook");
        HookStatus.failed("发送气泡",new IllegalStateException());
        check(!HookStatus.requiredReady(false),"settings entry alone does not prove sending works");
        System.out.println("OK diagnostics: per-feature evidence");
    }
    private static Field cacheField(String name)throws Exception {Field f=HostConfig.class.getDeclaredField(name);f.setAccessible(true);return f;}
    private static void drain()throws Exception {
        ScheduledThreadPoolExecutor worker=(ScheduledThreadPoolExecutor)cacheField("WRITES").get(null);
        for(int n=0;n<3;n++)worker.submit(() -> {}).get(5,TimeUnit.SECONDS);
    }
    @SuppressWarnings("unchecked") private static void cancelApplied()throws Exception {
        AtomicReference<ScheduledFuture<?>> ref=(AtomicReference<ScheduledFuture<?>>)cacheField("APPLIED_WRITE").get(null);
        ScheduledFuture<?> future=ref.getAndSet(null);if(future!=null)future.cancel(false);
    }
    private static void reset()throws Exception {
        cancelApplied();drain();
        cacheField("cache").set(null,Bundle.EMPTY);cacheField("sourceReadAt").setLong(null,-1L);
        cacheField("nextProviderProbe").setLong(null,0L);cacheField("providerAvailable").setBoolean(null,false);
        cacheField("providerFailures").setInt(null,0);cacheField("providerError").set(null,"");
        cacheField("lastHookReportTime").setLong(null,0L);
        ((AtomicBoolean)cacheField("REFRESHING").get(null)).set(false);
        ((AtomicLong)cacheField("GENERATION").get(null)).set(0L);
        SystemClock.now=100_000L;android.os.Binder.callingUid=1000;
        android.content.pm.PackageManager.packages=new String[]{"com.tencent.mobileqq"};
    }
    static final class Environment {
        volatile boolean offline;volatile int calls;volatile CountDownLatch entered,release;
        final FakeContext host=new FakeContext(),module=new FakeContext();final ConfigProvider provider=new ConfigProvider();
        Environment(){provider.testAttach(module);provider.onCreate();host.resolver=new ContentResolver(){
            @Override public Bundle call(Uri uri,String method,String arg,Bundle extras){
                calls++;
                if(offline)throw new IllegalArgumentException("Unknown authority "+AppConfig.AUTHORITY);
                CountDownLatch began=entered,finish=release;
                if(began!=null&&finish!=null){began.countDown();try{if(!finish.await(5,TimeUnit.SECONDS))throw new AssertionError("provider test gate timeout");}catch(InterruptedException e){throw new IllegalStateException(e);}}
                return provider.call(method,arg,extras);
            }
        };}
    }
    static final class FakeContext extends Context {
        final Map<String,MemoryPrefs> stores=new ConcurrentHashMap<>();ContentResolver resolver;
        MemoryPrefs prefs(String key){return stores.computeIfAbsent(key,k->new MemoryPrefs());}
        @Override public SharedPreferences getSharedPreferences(String name,int mode){return prefs(name);}
        @Override public ContentResolver getContentResolver(){return resolver;}
    }
    static final class MemoryPrefs implements SharedPreferences {
        final Map<String,Object> memory=new HashMap<>(),disk=new HashMap<>();boolean failCommit;int commits;
        @Override public Map<String,?> getAll(){return new HashMap<>(memory);}
        @Override public String getString(String key,String fallback){Object v=memory.get(key);return v==null?fallback:(String)v;}
        @Override public boolean getBoolean(String key,boolean fallback){Object v=memory.get(key);return v==null?fallback:(Boolean)v;}
        @Override public int getInt(String key,int fallback){Object v=memory.get(key);return v==null?fallback:(Integer)v;}
        @Override public long getLong(String key,long fallback){Object v=memory.get(key);return v==null?fallback:(Long)v;}
        void restart(){memory.clear();memory.putAll(disk);}
        @Override public Editor edit(){return new Editor(){
            final Map<String,Object> changes=new HashMap<>();final Set<String> removals=new HashSet<>();boolean clear;
            @Override public Editor putString(String k,String v){changes.put(k,v);return this;}
            @Override public Editor putBoolean(String k,boolean v){changes.put(k,v);return this;}
            @Override public Editor putInt(String k,int v){changes.put(k,v);return this;}
            @Override public Editor putLong(String k,long v){changes.put(k,v);return this;}
            @Override public Editor putFloat(String k,float v){changes.put(k,v);return this;}
            @Override public Editor putStringSet(String k,Set<String> v){changes.put(k,new HashSet<>(v));return this;}
            @Override public Editor remove(String k){removals.add(k);return this;}
            @Override public Editor clear(){clear=true;return this;}
            @Override public boolean commit(){commits++;if(clear)memory.clear();for(String k:removals)memory.remove(k);memory.putAll(changes);
                if(failCommit)return false;disk.clear();disk.putAll(memory);return true;}
            @Override public void apply(){commit();}
        };}
    }
}
