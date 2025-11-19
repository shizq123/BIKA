import com.shizq.bika.configureGraphTasks
import org.gradle.api.Plugin
import org.gradle.api.Project

class RootPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target.path == ":")
        target.subprojects { configureGraphTasks() }
    }
}