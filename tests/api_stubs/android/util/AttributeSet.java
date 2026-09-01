package android.util;
public interface AttributeSet {
public boolean getAttributeBooleanValue(String p0, String p1, boolean p2);
public boolean getAttributeBooleanValue(int p0, boolean p1);
public int getAttributeCount();
public float getAttributeFloatValue(String p0, String p1, float p2);
public float getAttributeFloatValue(int p0, float p1);
public int getAttributeIntValue(String p0, String p1, int p2);
public int getAttributeIntValue(int p0, int p1);
public int getAttributeListValue(String p0, String p1, String[] p2, int p3);
public int getAttributeListValue(int p0, String[] p1, int p2);
public String getAttributeName(int p0);
public int getAttributeNameResource(int p0);
public default String getAttributeNamespace(int p0) { throw new RuntimeException("API stub"); }
public int getAttributeResourceValue(String p0, String p1, int p2);
public int getAttributeResourceValue(int p0, int p1);
public int getAttributeUnsignedIntValue(String p0, String p1, int p2);
public int getAttributeUnsignedIntValue(int p0, int p1);
public String getAttributeValue(int p0);
public String getAttributeValue(String p0, String p1);
public String getClassAttribute();
public String getIdAttribute();
public int getIdAttributeResourceValue(int p0);
public String getPositionDescription();
public int getStyleAttribute();
}
