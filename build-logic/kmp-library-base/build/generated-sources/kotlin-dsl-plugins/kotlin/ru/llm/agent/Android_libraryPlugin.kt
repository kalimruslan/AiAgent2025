package ru.llm.agent


/**
 * Precompiled [android.library.gradle.kts][ru.llm.agent.Android_library_gradle] script plugin.
 *
 * @see ru.llm.agent.Android_library_gradle
 */
public
class Android_libraryPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("ru.llm.agent.Android_library_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
