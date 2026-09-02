package io.github.liuran001.mmliquidglass;

/** RGB is stored separately from opacity, including valid black (zero). */
final class RgbColor {
    static Integer parse(String value) {
        if(value==null) return null;
        String s=value.trim();
        if(s.startsWith("#")) s=s.substring(1);
        if(!s.matches("(?i)[0-9a-f]{3}|[0-9a-f]{6}")) return null;
        if(s.length()==3) s=""+s.charAt(0)+s.charAt(0)+s.charAt(1)+s.charAt(1)+s.charAt(2)+s.charAt(2);
        return Integer.parseInt(s,16);
    }
    static String format(int rgb) { return String.format(java.util.Locale.ROOT,"#%06X",rgb&0xffffff); }
}
