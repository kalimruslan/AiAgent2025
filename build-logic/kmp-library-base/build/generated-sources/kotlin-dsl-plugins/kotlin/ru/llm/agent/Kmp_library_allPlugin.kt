package ru.llm.agent


/**
 * Precompiled [kmp.library.all.gradle.kts][ru.llm.agent.Kmp_library_all_gradle] script plugin.
 *
 * @see ru.llm.agent.Kmp_library_all_gradle
 */
public
class Kmp_library_allPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("ru.llm.agent.Kmp_library_all_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
