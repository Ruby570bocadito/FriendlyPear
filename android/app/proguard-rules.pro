# FriendlyPear's release build (R8). What the manifest names (activities, services, receivers) is kept on its own;
# neither the app nor the core library looks anything up by name, so nothing else needs keeping.

# Crash reports with real line numbers, without the source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
