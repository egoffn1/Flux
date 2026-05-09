-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

-keep class com.fluxmusic.player.data.local.entity.** { *; }
-keep class com.fluxmusic.player.domain.model.** { *; }

-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer