package ru.llm.agent


/**
 * Precompiled [detekt.gradle.kts][ru.llm.agent.Detekt_gradle] script plugin.
 *
 * @see ru.llm.agent.Detekt_gradle
 */
public
class DetektPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("ru.llm.agent.Detekt_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
