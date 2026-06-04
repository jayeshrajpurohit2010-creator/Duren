# Duren Phase 0 — ProGuard / R8 rules.
# Most rules ship from library AARs (consumer ProGuard). This file holds the
# project-specific keep rules and stack-trace plumbing.

# Keep line numbers for Crashlytics stack traces, hide original source file.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin reflection metadata — needed for sealed classes, data classes, kotlinx.serialization.
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations
-keepattributes EnclosingMethod,InnerClasses,Signature,Exceptions

# kotlinx.serialization @Serializable nav route objects
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.duren.app.**$$serializer { *; }
-keepclassmembers class com.duren.app.** {
    *** Companion;
}

# Firestore data classes — uses reflection for serialization.
-keepclassmembers class com.duren.app.data.profile.model.** {
    <init>(...);
    <fields>;
}

# Hilt + Dagger generally don't need extra keep rules — the plugins ship them.
# Firebase libraries ship consumer ProGuard rules. Crashlytics consumes them.
