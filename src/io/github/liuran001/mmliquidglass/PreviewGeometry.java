package io.github.liuran001.mmliquidglass;

/** Preserve material/content size; adapt only vertical position to a short viewport. */
final class PreviewGeometry {
    static int offset(int height,int commonHeight,float density,int requestedDp) {
        int room=Math.max(0,height-commonHeight-Math.round(32*density));
        float fraction=Math.min(1f,room/Math.max(1f,100*density));
        return Math.round(Math.max(0,Math.min(100,requestedDp))*density*fraction);
    }
}
