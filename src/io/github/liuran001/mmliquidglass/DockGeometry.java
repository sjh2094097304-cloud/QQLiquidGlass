package io.github.liuran001.mmliquidglass;

/** Pure geometry, shared by the live layout and JVM regression tests. */
final class DockGeometry {
    final int left, barWidth, avatarLeft, avatarSize, gap;

    DockGeometry(int parentWidth, float density, int count) {
        int edge = Math.round(16 * density);
        avatarSize = Math.round(56 * density);
        gap = Math.round(24 * density);
        int available = Math.max(1, parentWidth - 2 * edge - avatarSize - gap);
        int desired = Math.round((count <= 3 ? 224 : 244) * density);
        barWidth = Math.min(desired, available);
        left = Math.max(0, (parentWidth - barWidth - avatarSize - gap) / 2);
        avatarLeft = left + barWidth + gap;
    }

    DockGeometry(int parentWidth,float density,int count,DockOptions o) {
        parentWidth=Math.max(1,parentWidth);
        int edge=Math.min(Math.round(12*density),parentWidth/4);
        int usable=Math.max(1,parentWidth-edge*2);
        float scale=o.scale();
        int desired=Math.round(Math.min(usable,640*density)*o.get(DockOptions.Key.WIDTH)/100f*scale);
        int total=Math.min(usable,Math.max(Math.min(usable,Math.round(180*density)),desired));
        avatarSize=o.on(DockOptions.Key.AVATAR)?Math.min(Math.round(o.get(DockOptions.Key.AVATAR_SIZE)*density*scale),total/3):0;
        gap=avatarSize>0?Math.min(Math.round(o.get(DockOptions.Key.GAP)*density*scale),total/8):0;
        barWidth=Math.max(1,total-avatarSize-gap);
        int shift=Math.round(o.get(DockOptions.Key.SHIFT)*density);
        left=Math.max(edge,Math.min(parentWidth-edge-total,(parentWidth-total)/2+shift));
        avatarLeft=left+barWidth+gap;
    }

    static String fallbackTitle(int slot, int count) {
        if (slot == 0) return "消息";
        if (slot == 1) return "联系人";
        if (count == 4 && slot == 2) return "频道";
        if (slot == count - 1) return "动态";
        return "标签 " + (slot + 1);
    }
}
