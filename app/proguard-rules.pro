# kotlinx-serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.miskibin.poznajswiat.**$$serializer { *; }
-keepclassmembers class com.miskibin.poznajswiat.** {
    *** Companion;
}
-keepclasseswithmembers class com.miskibin.poznajswiat.** {
    kotlinx.serialization.KSerializer serializer(...);
}
