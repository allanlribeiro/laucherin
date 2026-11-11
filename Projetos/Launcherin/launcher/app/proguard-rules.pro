# Shrink and optimise, but keep class and method names: the size win comes from dropping unused
# AndroidX code, not from renaming, and readable stack traces matter more in a sideloaded app
# where crash reports arrive as screenshots.
-dontobfuscate

# NewPipe uses Rhino reflectively to interpret streaming player scripts.
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**
# Desktop Bean introspection and invokedynamic optimizer are unused in interpreted mode.
-dontwarn org.mozilla.javascript.JavaToJSONConverters
-dontwarn jdk.dynalink.**
-keep class org.schabi.newpipe.extractor.timeago.patterns.** { *; }
