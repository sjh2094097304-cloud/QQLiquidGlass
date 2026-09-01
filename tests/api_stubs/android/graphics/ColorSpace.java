package android.graphics;
public abstract class ColorSpace {
protected ColorSpace() {}
 public static android.graphics.ColorSpace get(android.graphics.ColorSpace.Named p0) { throw new RuntimeException("API stub"); }
 public String getName() { throw new RuntimeException("API stub"); }
 public static android.graphics.ColorSpace match(float[] p0, android.graphics.ColorSpace.Rgb.TransferParameters p1) { throw new RuntimeException("API stub"); }
public enum Named {
ACES, ACESCG, ADOBE_RGB, BT2020, BT2020_HLG, BT2020_PQ, BT709, CIE_LAB, CIE_XYZ, DCI_P3, DISPLAY_BT2020, DISPLAY_P3, EXTENDED_SRGB, LINEAR_EXTENDED_SRGB, LINEAR_SRGB, NTSC_1953, OK_LAB, PRO_PHOTO_RGB, SMPTE_C, SRGB;
}
public static class Rgb extends android.graphics.ColorSpace {
protected Rgb() {}
public Rgb(String p0, float[] p1, java.util.function.DoubleUnaryOperator p2, java.util.function.DoubleUnaryOperator p3) {}
public Rgb(String p0, float[] p1, float[] p2, java.util.function.DoubleUnaryOperator p3, java.util.function.DoubleUnaryOperator p4, float p5, float p6) {}
public Rgb(String p0, float[] p1, android.graphics.ColorSpace.Rgb.TransferParameters p2) {}
public Rgb(String p0, float[] p1, float[] p2, android.graphics.ColorSpace.Rgb.TransferParameters p3) {}
public Rgb(String p0, float[] p1, double p2) {}
public Rgb(String p0, float[] p1, float[] p2, double p3) {}
public static class TransferParameters {
protected TransferParameters() {}
public TransferParameters(double p0, double p1, double p2, double p3, double p4) {}
public TransferParameters(double p0, double p1, double p2, double p3, double p4, double p5, double p6) {}
public final double a = 0;
public final double b = 0;
public final double c = 0;
public final double d = 0;
public final double e = 0;
public final double f = 0;
}
}
}
