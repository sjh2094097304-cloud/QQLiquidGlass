package io.github.liuran001.mmliquidglass;

public final class DockGeometryTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        for (float density : new float[]{1f, 1.25f, 1.5f, 2f, 2.625f, 3f, 3.5f, 4f}) {
            for (int widthDp : new int[]{280, 320, 360, 393, 411, 480, 600, 800, 1200}) {
                for (int n : new int[]{3, 4, 5}) {
                    int screen = Math.round(widthDp * density);
                    DockGeometry d = new DockGeometry(screen, density, n);
                    check(d.barWidth > 0, "bar width");
                    check(d.avatarLeft == d.left + d.barWidth + d.gap, "separate avatar geometry");
                    check(d.left >= Math.round(16 * density) - 1, "left safe edge");
                    check(d.avatarLeft + d.avatarSize <= screen - Math.round(16 * density) + 1, "right safe edge");
                    check(d.avatarSize >= 48 * density, "avatar touch target");
                    check(Math.abs(d.left - (screen - d.avatarLeft - d.avatarSize)) <= 1, "group centered");
                    check((d.barWidth - 8 * density) / n >= 28 * density, "minimum tab width");
                }
            }
        }
        check(DockGeometry.fallbackTitle(0, 3).equals("消息"), "three: messages");
        check(DockGeometry.fallbackTitle(1, 3).equals("联系人"), "three: contacts");
        check(DockGeometry.fallbackTitle(2, 3).equals("动态"), "three: activity");
        check(DockGeometry.fallbackTitle(2, 4).equals("频道"), "four: channels");
        check(DockGeometry.fallbackTitle(3, 4).equals("动态"), "four: activity");
        check(!DockGeometry.fallbackTitle(2, 5).equals("频道"), "unknown extra tabs are not mislabelled");
        System.out.println("PASS: " + checks + " geometry/title assertions (216 density/width/tab combinations)");
    }
}
