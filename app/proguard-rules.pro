-keep class com.obsidian.** { *; }
-keep class androidx.** { *; }
-keepclasseswithmembers class * {
    public <init>(...);
}
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
