package com.qiutian.bianpaobubble.hook;

import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** QQ's explicit VAS type 2 identifies bubbles. Never accepts bare or partial IDs. */
final class MallIdParser {
    private static final Pattern MARKER = Pattern.compile(
            "(?i)(?:^|[?&#;\\s{,])(?:S\\.)?[\"']?(itemid|item_id|kr_turbo_display)[\"']?\\s*[:=]\\s*[\"']?([^&#;\\s,\"'}\\]]+)");
    private static final Pattern DIRECT = Pattern.compile("2_([0-9]{4,10})");
    private MallIdParser() {}

    static boolean isItemKey(String key) {
        return "itemid".equalsIgnoreCase(key) || "item_id".equalsIgnoreCase(key)
                || "kr_turbo_display".equalsIgnoreCase(key);
    }
    static int direct(String input) {
        if (input == null) return 0;
        Matcher match = DIRECT.matcher(input.trim());
        return match.matches() ? id(match.group(1)) : 0;
    }
    static int parse(String input) {
        Selection result = new Selection();
        collect(input, result);
        return result.value();
    }
    static void collect(String input, Selection result) {
        if (input == null || input.length() > 32_768) return;
        String decoded = input;
        try {
            for (int i = 0; i < 2 && decoded.indexOf('%') >= 0; i++) decoded = URLDecoder.decode(decoded, "UTF-8");
        } catch (Exception ignored) { return; }
        Matcher match = MARKER.matcher(decoded);
        while (match.find()) collectValue(match.group(1), match.group(2), result);
    }
    static void collectValue(String key, String input, Selection result) {
        if (!isItemKey(key) || input == null) return;
        int found = direct(input);
        if (found > 0) result.add(found);
        else if (input.trim().matches("[0-9]+_.*")) result.ambiguous = true;
        // An unprefixed item ID supplies no type evidence. Ignore it; a separate
        // kr_turbo_display=2_ID may still identify the actual bubble correctly.
    }
    private static int id(String text) {
        try { int value = Integer.parseInt(text); return value >= 1000 ? value : 0; }
        catch (NumberFormatException ignored) { return 0; }
    }
    static final class Selection {
        int id;
        boolean ambiguous;
        void add(int value) {
            if (value <= 0) return;
            if (id != 0 && id != value) ambiguous = true;
            else id = value;
        }
        int value() { return ambiguous ? 0 : id; }
    }
}
