package com.qiutian.bianpaobubble.hook;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Minimal protobuf reader/rewriter for QQNT recall pushes; keeps the APK lightweight. */
final class ProtoLite {
    private ProtoLite() {}

    static boolean isRecallMsgPush(byte[] push) {
        byte[] message = firstBytes(push, 1);
        return message != null && isRecallQqMessage(message);
    }

    static boolean isSelfMsgPush(byte[] push) {
        byte[] message = firstBytes(push, 1);
        return isSelfQqMessage(message);
    }

    private static boolean isSelfQqMessage(byte[] message) {
        byte[] head = firstBytes(message, 1);
        String senderUid = firstString(head, 2);
        String receiverUid = firstString(head, 6);
        return senderUid != null && !senderUid.isEmpty() && senderUid.equals(receiverUid);
    }

    static RewriteResult stripSyncRecall(byte[] source) {
        if (source == null || source.length == 0) return new RewriteResult(source, false);
        return rewriteSync(source, 0);
    }

    /** InfoSyncPush[8] -> SyncMsgRecall[4] -> SyncInfoBody[8]. */
    private static RewriteResult rewriteSync(byte[] source, int level) {
        List<Field> fields = parse(source);
        ByteArrayOutputStream output = null;
        int selectedField = level == 1 ? 4 : 8;
        for (Field field : fields) {
            if (field.number == selectedField && field.wireType == 2) {
                byte[] content = Arrays.copyOfRange(source, field.payloadStart, field.payloadEnd);
                boolean remove = level == 2 && isRecallQqMessage(content) && !isSelfQqMessage(content);
                RewriteResult child = level < 2 ? rewriteSync(content, level + 1) : null;
                if (remove || child != null && child.changed) {
                    if (output == null) {
                        output = new ByteArrayOutputStream(source.length);
                        output.write(source, 0, field.start);
                    }
                    if (!remove) writeField(output, field.number, child.bytes);
                    continue;
                }
            }
            if (output != null) output.write(source, field.start, field.end - field.start);
        }
        return output == null ? new RewriteResult(source, false) : new RewriteResult(output.toByteArray(), true);
    }

    private static boolean isRecallQqMessage(byte[] message) {
        byte[] content = firstBytes(message, 2);
        long type = -1L;
        long subType = -1L;
        for (Field field : parse(content)) {
            if (field.wireType != 0) continue;
            if (field.number == 1) type = field.varintValue;
            else if (field.number == 2) subType = field.varintValue;
        }
        return (type == 732L && subType == 17L) || (type == 528L && subType == 138L);
    }

    private static byte[] firstBytes(byte[] source, int number) {
        if (source == null) return null;
        for (Field field : parse(source)) {
            if (field.number == number && field.wireType == 2) {
                return Arrays.copyOfRange(source, field.payloadStart, field.payloadEnd);
            }
        }
        return null;
    }

    private static String firstString(byte[] source, int number) {
        byte[] value = firstBytes(source, number);
        return value == null ? null : new String(value, StandardCharsets.UTF_8);
    }

    private static long firstVarint(byte[] source, int number, long fallback) {
        if (source == null) return fallback;
        for (Field field : parse(source)) {
            if (field.number == number && field.wireType == 0) return field.varintValue;
        }
        return fallback;
    }

    private static List<Field> parse(byte[] source) {
        List<Field> fields = new ArrayList<>();
        if (source == null) return fields;
        if (source.length > 4 * 1024 * 1024) throw new IllegalArgumentException("protobuf packet too large");
        int offset = 0;
        while (offset < source.length) {
            int start = offset;
            Varint tag = readVarint(source, offset);
            offset = tag.end;
            if ((tag.value >>> 3) > 536_870_911L) throw new IllegalArgumentException("protobuf tag out of range");
            int number = (int) (tag.value >>> 3);
            int wire = (int) (tag.value & 7L);
            if (number <= 0) throw new IllegalArgumentException("invalid protobuf field");
            int payloadStart = offset;
            int payloadEnd;
            long value = 0L;
            if (wire == 0) {
                Varint decoded = readVarint(source, offset);
                value = decoded.value;
                payloadEnd = decoded.end;
                offset = decoded.end;
            } else if (wire == 1) {
                payloadEnd = checkedEnd(offset, 8, source.length);
                offset = payloadEnd;
            } else if (wire == 2) {
                Varint length = readVarint(source, offset);
                if (length.value < 0 || length.value > Integer.MAX_VALUE) throw new IllegalArgumentException("protobuf field too large");
                payloadStart = length.end;
                payloadEnd = checkedEnd(payloadStart, (int) length.value, source.length);
                offset = payloadEnd;
            } else if (wire == 5) {
                payloadEnd = checkedEnd(offset, 4, source.length);
                offset = payloadEnd;
            } else {
                throw new IllegalArgumentException("unsupported protobuf wire type " + wire);
            }
            if (fields.size() >= 100_000) throw new IllegalArgumentException("too many protobuf fields");
            fields.add(new Field(number, wire, start, payloadStart, payloadEnd, offset, value));
        }
        return fields;
    }

    private static int checkedEnd(int start, int length, int limit) {
        long end = (long) start + length;
        if (length < 0 || end > limit) throw new IllegalArgumentException("truncated protobuf field");
        return (int) end;
    }

    private static Varint readVarint(byte[] source, int offset) {
        long result = 0L;
        for (int shift = 0; shift < 64 && offset < source.length; shift += 7) {
            int current = source[offset++] & 0xff;
            if (shift == 63 && (current & 0xfe) != 0) throw new IllegalArgumentException("protobuf varint overflow");
            result |= (long) (current & 0x7f) << shift;
            if ((current & 0x80) == 0) return new Varint(result, offset);
        }
        throw new IllegalArgumentException("invalid protobuf varint");
    }

    private static void writeField(ByteArrayOutputStream output, int number, byte[] payload) {
        writeVarint(output, ((long) number << 3) | 2L);
        writeVarint(output, payload.length);
        output.write(payload, 0, payload.length);
    }

    static byte[] field(int number, long value) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeVarint(output, (long) number << 3);
        writeVarint(output, value);
        return output.toByteArray();
    }

    static byte[] field(int number, byte[] payload) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeField(output, number, payload);
        return output.toByteArray();
    }

    static byte[] concat(byte[]... values) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (byte[] value : values) if (value != null) output.write(value, 0, value.length);
        return output.toByteArray();
    }

    private static void writeVarint(ByteArrayOutputStream output, long value) {
        while ((value & ~0x7fL) != 0) {
            output.write((int) (value & 0x7fL) | 0x80);
            value >>>= 7;
        }
        output.write((int) value);
    }

    static final class RewriteResult {
        final byte[] bytes;
        final boolean changed;

        RewriteResult(byte[] bytes, boolean changed) {
            this.bytes = bytes;
            this.changed = changed;
        }
    }

    private static final class Field {
        final int number;
        final int wireType;
        final int start;
        final int payloadStart;
        final int payloadEnd;
        final int end;
        final long varintValue;

        Field(int number, int wireType, int start, int payloadStart, int payloadEnd,
              int end, long varintValue) {
            this.number = number;
            this.wireType = wireType;
            this.start = start;
            this.payloadStart = payloadStart;
            this.payloadEnd = payloadEnd;
            this.end = end;
            this.varintValue = varintValue;
        }
    }

    private static final class Varint {
        final long value;
        final int end;

        Varint(long value, int end) {
            this.value = value;
            this.end = end;
        }
    }
}
