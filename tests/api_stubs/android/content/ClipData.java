package android.content;
public class ClipData {
protected ClipData() {}
public ClipData(CharSequence p0, String[] p1, android.content.ClipData.Item p2) {}
public ClipData(android.content.ClipDescription p0, android.content.ClipData.Item p1) {}
public ClipData(android.content.ClipData p0) {}
public android.content.ClipData.Item getItemAt(int p0) { throw new RuntimeException("API stub"); }
public int getItemCount() { throw new RuntimeException("API stub"); }
public static android.content.ClipData newPlainText(CharSequence p0, CharSequence p1) { throw new RuntimeException("API stub"); }
public static class Item {
protected Item() {}
public Item(CharSequence p0) {}
public Item(CharSequence p0, String p1) {}
public Item(android.content.Intent p0) {}
public Item(android.net.Uri p0) {}
public Item(CharSequence p0, android.content.Intent p1, android.net.Uri p2) {}
public Item(CharSequence p0, String p1, android.content.Intent p2, android.net.Uri p3) {}
public CharSequence coerceToText(android.content.Context p0) { throw new RuntimeException("API stub"); }
public android.content.Intent getIntent() { throw new RuntimeException("API stub"); }
public CharSequence getText() { throw new RuntimeException("API stub"); }
}
}
