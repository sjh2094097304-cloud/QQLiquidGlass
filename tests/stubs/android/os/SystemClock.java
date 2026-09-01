package android.os; public final class SystemClock { public static volatile long now=100_000L; public static long elapsedRealtime(){return now;} }
