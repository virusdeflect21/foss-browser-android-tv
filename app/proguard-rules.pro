# Schützt die Standard-Attribute von AndroidX, Java und GSON
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Verhindert, dass GSON Ihre Datenmodelle beim Schrumpfen unbrauchbar macht
-keep class de.baumann.browser.** { *; }

# Hält den Core von AndroidX-Bibliotheken stabil
-dontwarn androidx.**