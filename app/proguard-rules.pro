# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

##
## @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
#-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
#
##
##-keep class com.google.android.gms.internal.location.zze {
##    *;
##}
##
##-keep class androidx.compose.material3.pulltorefresh.PullToRefreshKt { *; }
##
#
#
#
## Gson rules
#-keepattributes Signature
#-keepattributes *Annotation*
##-keep class sun.misc.Unsafe { *; }
#-dontwarn sun.misc.Unsafe
#-dontwarn com.google.gson.**
#
#
#
## Retrofit rules
#-keepattributes Signature, InnerClasses, EnclosingMethod
#-keepclassmembers,allowshrinking,allowobfuscation interface * {
#    @retrofit2.http.* <methods>;
#}
#-dontwarn org.codehaus.mojo.animal_sniffer.*
#-dontwarn okio.**
#-dontwarn retrofit2.Platform$Java8

-dontobfuscate

# Your model classes
-keepclassmembers class com.despicable.core.model.** { *; }
-keepclassmembers class com.despicable.core.database.model** { *; }
-keepclassmembers class com.despicable.widgets.model** { *; }
-keepclassmembers class com.despicable.core.designsystem.theme** { *; }

#
#
## Keep inherited services.
#-if interface * { @retrofit2.http.* <methods>; }
#-keep,allowobfuscation interface * extends <1>
#
## With R8 full mode generic signatures are stripped for classes that are not
## kept. Suspend functions are wrapped in continuations where the type argument
## is used.
#-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
#
## R8 full mode strips generic signatures from return types if not kept.
#-if interface * { @retrofit2.http.* public *** *(...); }
#-keep,allowoptimization,allowshrinking,allowobfuscation class <3>
#
#
#
## Fix for Retrofit issue https://github.com/square/retrofit/issues/3751
## Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
##-keep,allowobfuscation,allowshrinking interface retrofit2.Call
##-keep,allowobfuscation,allowshrinking class retrofit2.Response
#
##

#
#-keep class com.yausername.** { *; }
#-keep class org.apache.commons.compress.archivers.zip.** { *; }
#
## Keep `Companion` object fields of serializable classes.
## This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
#-if @kotlinx.serialization.Serializable class **
#-keepclassmembers class <1> {
#    static <1>$Companion Companion;
#}
#
## Keep `serializer()` on companion objects (both default and named) of serializable classes.
#-if @kotlinx.serialization.Serializable class ** {
#    static **$* *;
#}
#-keepclassmembers class <2>$<3> {
#    kotlinx.serialization.KSerializer serializer(...);
#}
#
## Keep `INSTANCE.serializer()` of serializable objects.
#-if @kotlinx.serialization.Serializable class ** {
#    public static ** INSTANCE;
#}
#-keepclassmembers class <1> {
#    public static <1> INSTANCE;
#    kotlinx.serialization.KSerializer serializer(...);
#}
#
## @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
#-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
#
#
## Prevent R8 from removing icons used in goal icon picker.
#-keep class androidx.compose.material.icons.filled.** { *; }
#
## remove Log statements in release builds.
#-assumenosideeffects class android.util.Log {
#    public static *** d(...);
#    public static *** v(...);
#    public static *** i(...);
#    public static *** w(...);
#    public static *** e(...);
#}