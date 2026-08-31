# Log4j2 missing classes
-dontwarn aQute.bnd.annotation.spi.**
-dontwarn javax.management.**
-dontwarn javax.naming.**
-dontwarn javax.script.**
-dontwarn org.osgi.framework.**
-dontwarn org.apache.logging.log4j.core.jmx.**
-dontwarn org.apache.logging.log4j.core.net.JndiManager**

# Keep Log4j2 core classes
-keep class org.apache.logging.log4j.** { *; }
-keep interface org.apache.logging.log4j.** { *; }