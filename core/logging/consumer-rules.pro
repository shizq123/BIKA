# Log4j2 & SLF4J 混淆规则
-dontwarn aQute.bnd.annotation.spi.**
-dontwarn java.lang.management.**
-dontwarn javax.management.**
-dontwarn javax.naming.**
-dontwarn javax.script.**
-dontwarn org.osgi.framework.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.slf4j.**

-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, Exceptions
-keep class org.apache.logging.log4j.** { *; }
-keep interface org.apache.logging.log4j.** { *; }
-keep enum org.apache.logging.log4j.** { *; }
-keep class * implements org.apache.logging.log4j.spi.Provider { *; }
-keep class * implements org.slf4j.spi.SLF4JServiceProvider { *; }
-keep class * implements org.apache.logging.log4j.core.config.plugins.util.PluginType { *; }
