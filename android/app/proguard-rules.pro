# FriendlyPear's release build (R8). What the manifest names (activities, services, receivers) is kept on its own;
# neither the app nor the core library looks anything up by name, so nothing else needs keeping.

# No log in the release build: what Peri does (a call, a shield, the family's state) stays out of the phone's log,
# which a computer plugged in over USB can read. R8 drops the calls and the text built for them.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# Crash reports with real line numbers, without the source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
