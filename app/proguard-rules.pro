# Keep llama JNI classes
-keep class com.cunyi.doctor.llm.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
