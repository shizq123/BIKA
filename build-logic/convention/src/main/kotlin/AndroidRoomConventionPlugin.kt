import com.google.devtools.ksp.gradle.KspExtension
import com.shizq.bika.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidRoomConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "androidx.room")
            apply(plugin = "com.google.devtools.ksp")

            extensions.configure<KspExtension> {
                arg("room.generateKotlin", "true")
            }

            val room = extensions.findByName("room") as? Any
            if (room != null) {
                try {
                    val roomClazz = Class.forName("androidx.room.gradle.RoomExtension")
                    val method = roomClazz.getMethod("schemaDirectory", String::class.java)
                    method.invoke(room, "$projectDir/schemas")
                } catch (e: Throwable) {
                    val method = room::class.java.methods.firstOrNull { 
                        it.name == "schemaDirectory" && 
                        it.parameterCount == 1 && 
                        it.parameterTypes[0] == String::class.java &&
                        !java.lang.reflect.Modifier.isStatic(it.modifiers)
                    }
                    if (method != null) {
                        method.invoke(room, "$projectDir/schemas")
                    } else {
                        throw e
                    }
                }
            }

            dependencies {
                "implementation"(libs.findLibrary("androidx.room.runtime").get())
                "implementation"(libs.findLibrary("androidx.room.ktx").get())
                "ksp"(libs.findLibrary("androidx.room.compiler").get())
            }
        }
    }
}